package tn.esprit.candidat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/candidats")
public class CandidatController {

    private final CandidatRepository repo;

    @Value("${server.port}")
    private String port;

    public CandidatController(CandidatRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Candidat> all() { return repo.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Candidat> one(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Candidat create(@RequestBody Candidat c) { c.setId(null); return repo.save(c); }

    @PutMapping("/{id}")
    public ResponseEntity<Candidat> update(@PathVariable Long id, @RequestBody Candidat c) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        c.setId(id);
        return ResponseEntity.ok(repo.save(c));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Utile pour observer la répartition de charge entre instances. */
    @GetMapping("/instance")
    public Map<String, String> instance() {
        return Map.of("service", "candidat", "port", port);
    }
}
