package edu.eci.arsw.blueprints.controllers;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/blueprints")
public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) { this.services = services; }

    // GET /api/v1/blueprints -> 200 OK | 404 Not Found (no hay planos registrados)
    @GetMapping
    public ResponseEntity<?> getAll() {
        Set<Blueprint> all = services.getAllBlueprints();
        if (all.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No blueprints found"));
        }
        return ResponseEntity.ok(all);
    }

    // GET /api/v1/blueprints/{author} -> 200 OK | 404 Not Found
    @GetMapping("/{author}")
    public ResponseEntity<?> byAuthor(@PathVariable String author) {
        try {
            return ResponseEntity.ok(services.getBlueprintsByAuthor(author));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/v1/blueprints/{author}/{bpname} -> 200 OK | 404 Not Found
    @GetMapping("/{author}/{bpname}")
    public ResponseEntity<?> byAuthorAndName(@PathVariable String author, @PathVariable String bpname) {
        try {
            return ResponseEntity.ok(services.getBlueprint(author, bpname));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/v1/blueprints -> 201 Created | 400 Bad Request (datos invalidos o plano ya existente)
    @PostMapping
    public ResponseEntity<?> add(@Valid @RequestBody NewBlueprintRequest req) {
        try {
            List<Point> points = req.points() == null
                    ? List.of()
                    : req.points().stream().map(PointRequest::toPoint).toList();
            services.addNewBlueprint(new Blueprint(req.author(), req.name(), points));
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (BlueprintPersistenceException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/v1/blueprints/{author}/{bpname}/points -> 202 Accepted | 400 Bad Request | 404 Not Found
    @PutMapping("/{author}/{bpname}/points")
    public ResponseEntity<?> addPoint(@PathVariable String author, @PathVariable String bpname,
                                      @Valid @RequestBody PointRequest p) {
        try {
            services.addPoint(author, bpname, p.x(), p.y());
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    public record NewBlueprintRequest(
            @NotBlank String author,
            @NotBlank String name,
            List<@NotNull @Valid PointRequest> points
    ) { }

    /** Punto recibido en el body. Usa Integer para detectar x/y ausentes o nulos (400) en vez de asumir 0. */
    public record PointRequest(
            @NotNull Integer x,
            @NotNull Integer y
    ) {
        public Point toPoint() { return new Point(x, y); }
    }
}
