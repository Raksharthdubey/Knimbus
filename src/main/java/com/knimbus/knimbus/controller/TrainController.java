package com.knimbus.knimbus.controller;

import com.knimbus.knimbus.view.Train;
import com.knimbus.knimbus.service.TrainService;
import com.knimbus.knimbus.exception.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;


@RestController
public class TrainController {

    @Autowired
    private TrainService trainService;
    @GetMapping("/trains")
    public ResponseEntity<Map<String, List<Train>>> getNextTrains() {
        try {
            Map<String, List<Train>> trainMap = trainService.getNextTrains();
            return ResponseEntity.ok(trainMap);

        } catch (CustomException ce ) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found", ce);
        } catch (Exception e ) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Generic exception. Please check stacktrace.", e);
        }

    }
}
