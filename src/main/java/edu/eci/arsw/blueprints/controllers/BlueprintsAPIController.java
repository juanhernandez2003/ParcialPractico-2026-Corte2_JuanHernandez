package edu.eci.arsw.blueprints.controllers;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * API REST de planos. Todas las respuestas usan ApiResponse.
 * Los errores (404 y 400) no se manejan aqui: las excepciones las atrapa GlobalExceptionHandler.
 *
 * Nota: la anotacion de Swagger se escribe con su nombre completo
 * (@io.swagger.v3.oas.annotations.responses.ApiResponse) porque nuestra clase
 * de respuesta tambien se llama ApiResponse.
 */
@RestController
@RequestMapping("/api/v1/blueprints")
@Tag(name = "Blueprints", description = "Consulta, creacion y actualizacion de planos")
public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) {
        this.services = services;
    }

    // GET /api/v1/blueprints -> 200 OK | 404 si no hay planos
    @Operation(summary = "Listar todos los planos")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Planos encontrados"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No hay planos registrados")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Set<Blueprint>>> getAll() throws BlueprintNotFoundException {
        Set<Blueprint> all = services.getAllBlueprints();
        if (all.isEmpty()) {
            throw new BlueprintNotFoundException("No blueprints found");
        }
        return ResponseEntity.ok(new ApiResponse<>(200, "execute ok", all));
    }

    // GET /api/v1/blueprints/{author} -> 200 OK | 404
    @Operation(summary = "Listar los planos de un autor")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Planos del autor encontrados"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "El autor no tiene planos")
    })
    @GetMapping("/{author}")
    public ResponseEntity<ApiResponse<Set<Blueprint>>> byAuthor(@PathVariable String author)
            throws BlueprintNotFoundException {
        Set<Blueprint> blueprints = services.getBlueprintsByAuthor(author);
        return ResponseEntity.ok(new ApiResponse<>(200, "execute ok", blueprints));
    }

    // GET /api/v1/blueprints/{author}/{bpname} -> 200 OK | 404
    @Operation(summary = "Consultar un plano por autor y nombre")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Plano encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "El plano no existe")
    })
    @GetMapping("/{author}/{bpname}")
    public ResponseEntity<ApiResponse<Blueprint>> byAuthorAndName(@PathVariable String author,
                                                                  @PathVariable String bpname)
            throws BlueprintNotFoundException {
        Blueprint bp = services.getBlueprint(author, bpname);
        return ResponseEntity.ok(new ApiResponse<>(200, "execute ok", bp));
    }

    // POST /api/v1/blueprints -> 201 Created | 400 (datos invalidos o plano repetido)
    @Operation(summary = "Crear un plano nuevo")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Plano creado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos invalidos o el plano ya existe")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Blueprint>> add(@Valid @RequestBody NewBlueprintRequest req)
            throws BlueprintPersistenceException {
        Blueprint bp = new Blueprint(req.author(), req.name(), req.toPoints());
        services.addNewBlueprint(bp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(201, "blueprint created", bp));
    }

    // PUT /api/v1/blueprints/{author}/{bpname}/points -> 202 Accepted | 400 | 404
    @Operation(summary = "Agregar un punto a un plano existente")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Punto agregado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Punto invalido (falta x o y)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "El plano no existe")
    })
    @PutMapping("/{author}/{bpname}/points")
    public ResponseEntity<ApiResponse<Blueprint>> addPoint(@PathVariable String author,
                                                           @PathVariable String bpname,
                                                           @Valid @RequestBody PointRequest p)
            throws BlueprintNotFoundException {
        services.addPoint(author, bpname, p.x(), p.y());
        Blueprint updated = services.getBlueprint(author, bpname);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>(202, "point added", updated));
    }

    // ---------- Cuerpos de las peticiones (se validan con @Valid) ----------

    /** Body del POST. author y name son obligatorios; points es opcional. */
    public record NewBlueprintRequest(
            @NotBlank String author,
            @NotBlank String name,
            List<@NotNull @Valid PointRequest> points
    ) {
        public List<Point> toPoints() {
            if (points == null) {
                return List.of();
            }
            return points.stream().map(p -> new Point(p.x(), p.y())).toList();
        }
    }

    /** Body del PUT. Integer (no int) para que un x o y ausente de 400 en vez de tomarse como 0. */
    public record PointRequest(
            @NotNull Integer x,
            @NotNull Integer y
    ) { }
}
