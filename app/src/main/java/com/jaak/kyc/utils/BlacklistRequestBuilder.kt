package com.jaak.kyc.utils

import com.jaak.kyc.data.model.blacklist.*
import com.jaak.kyc.data.model.ocr.v4.DocumentExtractV4Response

object BlacklistRequestBuilder {

    /**
     * Crea un payload de blacklist a partir del response de Document Extract V4
     */
    fun createPayloadFromDocumentExtract(ocrResponse: DocumentExtractV4Response): BlacklistPayload {
        val personal = ocrResponse.content.data.personal
        val address = ocrResponse.content.data.address
        val document = ocrResponse.content.data.document

        return BlacklistPayload(
            person = BlacklistPerson(
                name = personal.firstName ?: "",
                lastName = personal.surname ?: "",
                secondName = personal.secondName ?: "",
                secondLastName = personal.motherSurname ?: "",
                birthDate = personal.dateOfBirth ?: "",
                nationality = personal.nationality ?: ""
            ),
            address = BlacklistAddress(
                address = address?.extra?.street ?: address?.fullAddress ?: "",
                number = address?.extra?.externalNumber?.toIntOrNull() ?: 0,
                neighborhood = address?.extra?.neighborhood ?: "",
                municipality = "", // No disponible en Document Extract V4
                city = address?.extra?.city ?: "",
                state = address?.extra?.state ?: "",
                postalCode = address?.postalCode ?: ""
            ),
            identifications = BlacklistIdentifications(
                curp = document.personalIdNumber ?: "",
                rfc = personal.extra?.rfc ?: "",
                socialSecurityNumber = "",
                electorKey = if (document.type == "I") document.number ?: "" else "", // Clave de elector si es INE
                ine = if (document.type == "I" && (personal.extra?.ocr != null || document.additionalNumber != null)) {
                    BlacklistIne(
                        cic = document.additionalNumber ?: "",
                        ocr = personal.extra?.ocr ?: ""
                    )
                } else null
            ),
            extras = BlacklistExtras(
                commonId = "",
                wantedIn = ""
            )
        )
    }
    
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