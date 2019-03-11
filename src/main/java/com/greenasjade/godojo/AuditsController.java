package com.greenasjade.godojo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuditsController {

	@Value("${godojo.http.ogs-key}")
	private String ogs_key;

    private static final Logger log = LoggerFactory.getLogger(AuditsController.class);

    private BoardPositionStore bp_store;

    public AuditsController(
            BoardPositionStore bp_store) {
        this.bp_store = bp_store;
    }

    @CrossOrigin()
    @ResponseBody()
    @GetMapping("/godojo/audits" )
    // Return all the information needed to display audit log for a position
    public AuditLogDTO audits(
            @RequestParam(value = "id", required = false, defaultValue = "root") String id) {

        BoardPosition board_position;

        if (id.equals("root")) {
            board_position = this.bp_store.findByPlay(".root");
            id = board_position.id.toString();
        }
        else {
            board_position = this.bp_store.findById(Long.valueOf(id)).orElse(null);
        }

        log.info("Audit request for: " + id);

        log.info("which is: " + board_position.toString());

        AuditLogDTO audits = new AuditLogDTO(board_position);

        return audits;
    }
}