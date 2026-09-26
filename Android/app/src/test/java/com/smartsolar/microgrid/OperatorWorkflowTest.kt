package com.smartsolar.microgrid

import com.google.gson.Gson
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.smartsolar.microgrid.data.repository.OperatorRepository
import com.smartsolar.microgrid.model.TransferDetails
import org.junit.Assert.*
import org.junit.Test

class OperatorWorkflowTest {
    @Test fun secureTokenSurvivesQrEncodingAndScanning() {
        val token = "TRX:" + "A13B".repeat(16)
        val matrix = QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 400, 400)
        val pixels = IntArray(400 * 400) { i -> if (matrix[i % 400, i / 400]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(400, 400, pixels)))
        assertEquals(token, MultiFormatReader().decode(bitmap).text)
    }

    @Test fun authoritativeResponseKeepsNicAndCompletionMetadata() {
        val parsed = Gson().fromJson("""{"transactionId":"TRX-1","reservationId":"RES-1",
            "prosumerNIC":"991234567V","prosumerName":"Test Prosumer","stationId":"ST1",
            "stationName":"Solar Station","slotId":"SL1","scheduledStartTime":"2026-09-26T10:00:00Z",
            "scheduledEndTime":"2026-09-26T11:00:00Z","energyAmountKwh":5.5,
            "reservationStatus":"COMPLETED","transactionStatus":"COMPLETED","qrStatus":"USED",
            "completedAt":"2026-09-26T10:30:00Z"}""", TransferDetails::class.java)
        assertEquals("991234567V", parsed.prosumerNIC)
        assertEquals("COMPLETED", parsed.transactionStatus)
        assertEquals("USED", parsed.qrStatus)
        assertNotNull(parsed.completedAt)
    }

    @Test fun errorsHaveSafeActionableMessages() {
        assertTrue(OperatorRepository.messageFor(401).contains("log in"))
        assertTrue(OperatorRepository.messageFor(403).contains("assignment"))
        assertTrue(OperatorRepository.messageFor(409).contains("completed"))
        assertFalse(OperatorRepository.messageFor(500).contains("Exception"))
    }
}
