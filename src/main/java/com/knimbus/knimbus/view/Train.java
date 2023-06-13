package com.knimbus.knimbus.view;

import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Train {
    private String line;
    private String destination;
    private Date departureTime;

    private String output;

    // Constructor, getters, and setters

    @Override
    public String toString() {
        return line + ": Departing in " + departureTime + " minutes to " + destination;
    }
}

