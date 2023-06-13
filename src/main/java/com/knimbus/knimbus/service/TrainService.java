package com.knimbus.knimbus.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.knimbus.knimbus.exception.CustomException;
import com.knimbus.knimbus.view.Train;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrainService {
    private static final String MBTA_API_URL = "https://api-v3.mbta.com/schedules?filter[stop]=place-pktrm&filter[direction_id]=0&include=stop";

    private static final Logger LOGGER = LoggerFactory.getLogger(TrainService.class);
    public Map<String, List<Train>> getNextTrains() throws CustomException {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>("", headers);
            ResponseEntity<String> response = restTemplate.exchange(MBTA_API_URL, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(response.getBody(), JsonObject.class);
                JsonArray data = jsonObject.getAsJsonArray("data");

                List<Train> trains = new ArrayList<>();
                for (JsonElement element : data) {
                    JsonObject trainData = element.getAsJsonObject();
                    String line = trainData.get("relationships").getAsJsonObject().get("route").getAsJsonObject().get("data").getAsJsonObject().get("id").getAsString();
                    String destination = trainData.get("relationships").getAsJsonObject().get("stop").getAsJsonObject().get("data").getAsJsonObject().get("id").getAsString();
                    String departureTime = trainData.get("attributes").getAsJsonObject().get("departure_time").getAsString();

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

                    Train train = new Train();
                    train.setLine(line);
                    train.setDestination(destination);
                    try {
                        Date date = dateFormat.parse(departureTime);
                        train.setDepartureTime(date);
                    } catch (ParseException e) {
                       LOGGER.error(e.getMessage());
                    }

                    LOGGER.info(train);
                    trains.add(train);
                }

                // Sort the trains by departure time
                trains.sort(Comparator.comparing(Train::getDepartureTime));

                List<Train> finalTrains = processTrains(trains).stream()
                        .limit(10)
                        .collect(Collectors.toList());

                Map<String, List<Train>> trainMap = finalTrains.stream()
                        .collect(Collectors.groupingBy(Train::getLine,
                                Collectors.mapping(train -> train, Collectors.toList())))
                        .entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()));


                return trainMap;

            } else {
                // Log the error response
                LOGGER.error("Failed to fetch train data from MBTA API. Response status: {}", response.getStatusCode());
            }
        }catch (CustomException ce) {
            // Log any exceptions that occur during API call
            throw new CustomException("Error in processing trains");
        }
        catch (Exception e) {
            // Log any exceptions that occur during API call
            LOGGER.error("An error occurred while fetching train data from MBTA API", e);
        }

        return Collections.emptyMap();
    }

    private List<Train> processTrains(List<Train> trains) throws CustomException {
        if (!trains.isEmpty()) {
            // Get the current time in UTC-4 timezone
            LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("UTC-4"));
            Date date = Date.from(currentTime.atZone(ZoneId.systemDefault()).toInstant());

            // Group the trains by line
            Map<String, List<Train>> trainsByLine = trains.stream().collect(Collectors.groupingBy(Train::getLine));

            // Sort the trains by departure time within each line
            trainsByLine.forEach((line, lineTrains) ->
                    lineTrains.sort(Comparator.comparing(Train::getDepartureTime)));


            for (Map.Entry<String, List<Train>> entry : trainsByLine.entrySet()) {
                String line = entry.getKey();
                List<Train> trainsOnLine = entry.getValue();

                for (Train train : trainsOnLine) {

                    if (train.getDepartureTime().compareTo(date) > 0) {
                        long differenceInMillis = train.getDepartureTime().getTime() - date.getTime();
                        long differenceInMinutes = differenceInMillis / (60 * 1000);
                        StringBuilder output = new StringBuilder();
                        output.append("Departs in " + differenceInMinutes + " minutes.");
                        train.setOutput(String.valueOf(output));
                    }
                    else {
                        trainsOnLine.remove(train);
                    }
                }
                return trainsOnLine;
            }

        }
        throw new CustomException("Data not Found");
    }


}


