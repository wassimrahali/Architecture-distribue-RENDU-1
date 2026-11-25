package tn.esprit.job;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobRepository repo;
    private final RestTemplate restTemplate;

    public JobController(JobRepository repo, RestTemplate restTemplate) {
        this.repo = repo;
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public List<Job> all() { return repo.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Job> one(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Job create(@RequestBody Job j) { j.setId(null); return repo.save(j); }

    @PutMapping("/{id}")
    public ResponseEntity<Job> update(@PathVariable Long id, @RequestBody Job j) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        j.setId(id);
        return ResponseEntity.ok(repo.save(j));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Démontre la découverte de services : appelle "candidat" par son NOM (pas d'adresse en dur).
     * Si plusieurs instances existent, la charge est répartie entre elles.
     */
    @GetMapping("/candidats-instance")
    public String candidatInstance() {
        return restTemplate.getForObject("http://candidat/candidats/instance", String.class);
    }
}
