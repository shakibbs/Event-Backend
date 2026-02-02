package com.event_management_system.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenPayloadDTO {

    
    private String subject;

    private String tokenUuid;

    private Date issuedAt;

    private Date expiration;
}
