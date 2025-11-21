package com.acl.backend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.acl.backend.data.AnalysisData;
import com.acl.backend.model.Contract;
import com.acl.backend.model.DeletedContract;
import com.acl.backend.model.User;
import com.acl.backend.repository.ChatRepository;
import com.acl.backend.repository.DeletedContractRepository;
import com.acl.backend.repository.UserRepository;
import com.acl.backend.service.ContractService;
import com.acl.backend.service.NLPAnalysisService;
import com.acl.backend.service.ReportService;
import com.acl.backend.service.TextExtractionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;
    private final TextExtractionService textExtractionService;
    private final NLPAnalysisService nlpAnalysisService;
    private final ReportService reportService;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final DeletedContractRepository deletedContractRepository;

    public ContractController(ContractService contractService,
                              TextExtractionService textExtractionService,
                              NLPAnalysisService nlpAnalysisService,
                              ReportService reportService,
                              UserRepository userRepository,
                              ChatRepository chatRepository,
                              DeletedContractRepository deletedContractRepository) {
        this.contractService = contractService;
        this.textExtractionService = textExtractionService;
        this.nlpAnalysisService = nlpAnalysisService;
        this.reportService = reportService;
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
        this.deletedContractRepository = deletedContractRepository;
    }

    // Sube y analiza un contrato (PDF/DOCX)

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisData.UploadResponse> upload (
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @AuthenticationPrincipal UserDetails userDetails
    ) throws Exception {

        // Extrar texto del archivo
        String text = textExtractionService.extractText(file);
        String contractName = name != null ? name : file.getOriginalFilename();

        // Obtener userId del usuario autenticado
        Long userId = null;
        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            userId = user.getId();
        }

        // Guardar y analizar
        Contract saved = contractService.saveWithAnalysis(contractName, text, userId);
        AnalysisData.AnalysisResult analysis = nlpAnalysisService.analyze(text);

        AnalysisData.UploadResponse resp = new AnalysisData.UploadResponse();
        resp.setContractId(saved.getId());
        resp.setAnalysis(analysis);

        return ResponseEntity.ok(resp);
    }

    // Eliminar contrato y registrar en "deleted_contracts"
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Contract contract = contractService.findById(id).orElse(null);
        if (contract == null) return ResponseEntity.notFound().build();

        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            if (contract.getUserId() != null && !contract.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
        }

        chatRepository.deleteByContractId(id);
        contractService.deleteById(id);

        DeletedContract dc = new DeletedContract();
        dc.setContractId(id);
        dc.setUserId(contract.getUserId());
        dc.setName(contract.getName());
        dc.setDeletedAt(java.time.Instant.now());
        try { deletedContractRepository.save(dc); } catch (Exception ignored) { }

        return ResponseEntity.ok().build();
    }

    // Analiza texto directo sin subir archivo
    @PostMapping(value = "/analyze-text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisData.AnalysisResult> analyzeText (
            @Valid @RequestBody AnalysisData.AnalyzeTextRequest req) {

        AnalysisData.AnalysisResult result = nlpAnalysisService.analyze(req.getText());
        return ResponseEntity.ok(result);
    }

    // Lista todos los contratos del usuario autenticado
    @GetMapping
    public ResponseEntity<List<Contract>> listAll(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(contractService.listAll());
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return ResponseEntity.ok(contractService.listByUser(user.getId()));
    }

    // Obtener un contrato por ID
    @GetMapping("/{id}")
    public ResponseEntity<Contract> getById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Contract contract = contractService.findById(id)
                .orElse(null);

        if (contract == null) {
            return ResponseEntity.notFound().build();
        }

        // Validar que el contrato pertenezca al usuario
        if (userDetails != null) {
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (contract.getUserId() != null && !contract.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).build(); // Forbidden
            }
        }

        return ResponseEntity.ok(contract);
    }

    // Generar Reporte PDF del análisis de un contrato
    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> getReport(@PathVariable String id) throws Exception {
        var contract = contractService.findById(id).orElse(null);
        if (contract == null) return ResponseEntity.notFound().build();

        var analysis = nlpAnalysisService.analyze(contract.getContent());
        byte[] pdf = reportService.generatePdf(contract, analysis);

        String filename = URLEncoder.encode("reporte-" + contract.getName() + ".pdf", StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // Q&A Responde preguntas sobre un contrato
    @GetMapping("/{id}/qa")
    public ResponseEntity<List<String>> qa(
            @PathVariable String id,
            @RequestParam("q") String question
    ) {
        var contract = contractService.findById(id).orElse(null);
        if (contract == null) return ResponseEntity.notFound().build();

        var answers = nlpAnalysisService.answerQuestions(contract.getContent(), question);
        return ResponseEntity.ok(answers);
    }


}
