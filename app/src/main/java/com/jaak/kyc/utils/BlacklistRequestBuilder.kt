package com.jaak.kyc.utils

import com.jaak.kyc.data.model.blacklist.*

object BlacklistRequestBuilder {
    
    /**
     * Crea un request para investigación INE
     */
    fun createIneRequest(payload: BlacklistPayload): BlacklistRequest {
        return BlacklistRequest(
            services = BlacklistServices(ine = true),
            payload = payload
        )
    }
    
    /**
     * Crea un request para investigación INTERPOL
     */
    fun createInterpolRequest(payload: BlacklistPayload): BlacklistRequest {
        return BlacklistRequest(
            services = BlacklistServices(interpol = true),
            payload = payload
        )
    }
    
    /**
     * Crea un request para investigación OFAC
     */
    fun createOfacRequest(payload: BlacklistPayload): BlacklistRequest {
        return BlacklistRequest(
            services = BlacklistServices(ofac = true),
            payload = payload
        )
    }
    
    /**
     * Crea un request para investigación RENAPO
     */
    fun createRenapoRequest(payload: BlacklistPayload): BlacklistRequest {
        return BlacklistRequest(
            services = BlacklistServices(
                renapo = BlacklistRenapoService(curp = true)
            ),
            payload = payload
        )
    }
    
    /**
     * Crea un request para investigación SAT
     */
    fun createSatRequest(payload: BlacklistPayload): BlacklistRequest {
        return BlacklistRequest(
            services = BlacklistServices(
                sat = BlacklistSatService(sat69b = true)
            ),
            payload = payload
        )
    }
    
    /**
     * Crea un payload base a partir de datos del OCR y otros servicios
     */
    fun createBasePayload(
        // Datos de persona
        name: String = "",
        lastName: String = "",
        secondName: String = "",
        secondLastName: String = "",
        birthDate: String = "",
        nationality: String = "",
        
        // Datos de dirección (del geocoding)
        address: String = "",
        number: Int = 0,
        neighborhood: String = "",
        municipality: String = "",
        city: String = "",
        state: String = "",
        postalCode: String = "",
        
        // Identificaciones (del OCR)
        curp: String = "",
        rfc: String = "",
        socialSecurityNumber: String = "",
        electorKey: String = "",
        ineCic: String = "",
        ineOcr: String = "",
        
        // Extras
        commonId: String = "",
        wantedIn: String = ""
    ): BlacklistPayload {
        return BlacklistPayload(
            person = BlacklistPerson(
                name = name,
                lastName = lastName,
                secondName = secondName,
                secondLastName = secondLastName,
                birthDate = birthDate,
                nationality = nationality
            ),
            address = BlacklistAddress(
                address = address,
                number = number,
                neighborhood = neighborhood,
                municipality = municipality,
                city = city,
                state = state,
                postalCode = postalCode
            ),
            identifications = BlacklistIdentifications(
                curp = curp,
                rfc = rfc,
                socialSecurityNumber = socialSecurityNumber,
                electorKey = electorKey,
                ine = if (ineCic.isNotEmpty() || ineOcr.isNotEmpty()) {
                    BlacklistIne(cic = ineCic, ocr = ineOcr)
                } else null
            ),
            extras = BlacklistExtras(
                commonId = commonId,
                wantedIn = wantedIn
            )
        )
    }
}