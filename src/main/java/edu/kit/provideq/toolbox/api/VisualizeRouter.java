package edu.kit.provideq.toolbox.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static edu.kit.provideq.toolbox.featuremodel.svgconvert.UvlToImageConverter.visualize;

@RestController
@RequestMapping("/visualize")
public class VisualizeRouter {

    @PostMapping("/feature-model")
    public ResponseEntity<String> visualizeFeatureModel(@RequestBody Map<String, String> payload) {
        String uvl = payload.get("uvl");  // Extracting the UVL string from the map
        System.out.println("Received UVL: " + uvl);
        // Simulate SVG generation
        String svg = visualize(uvl);

        // Set content type as SVG
        return ResponseEntity.ok()
                .header("Content-Type", "image/svg+xml")
                .body(svg);
    }

}
