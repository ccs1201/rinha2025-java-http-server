//package br.com.ccs.rinha.model.input;
//
//
//import java.nio.charset.StandardCharsets;
//
//class PaymentRequestTest {
//
//    public static void main(String[] args) {
//        String originalJsonString = "{\n" +
//                "    \"correlationId\": \"4a7901b8-7d26-4d9d-aa19-4dc1c7cf60b3\",\n" +
//                "    \"amount\": 19.90\n" +
//                "}";
//
//        byte[] originalJsonBytes = originalJsonString.getBytes(StandardCharsets.UTF_8);
//
//        System.out.println("JSON Original:\n" + new String(originalJsonBytes, StandardCharsets.UTF_8));
//
//        byte[] modifiedJsonBytes = new PaymentRequest(originalJsonBytes).addRequestedAtToJsonBytes(originalJsonBytes);
//
//        System.out.println("\nJSON Modificado:\n" + new String(modifiedJsonBytes, StandardCharsets.UTF_8));
//
//        // Exemplo com JSON em uma única linha
//        String singleLineJson = "{\"correlationId\":\"abc\",\"amount\":10.0}";
//        byte[] singleLineBytes = singleLineJson.getBytes(StandardCharsets.UTF_8);
//        byte[] modifiedSingleLine = new PaymentRequest(singleLineBytes).addRequestedAtToJsonBytes(singleLineBytes);
//        System.out.println("\nJSON em uma linha (original): " + new String(singleLineBytes, StandardCharsets.UTF_8));
//        System.out.println("JSON em uma linha (modificado): " + new String(modifiedSingleLine, StandardCharsets.UTF_8));
//
//
//        var paymentDefault = new PaymentRequest.parseToDefault(modifiedJsonBytes);
//        var paymentFallback = PaymentRequest.parseToFallback(modifiedJsonBytes);
//
//        if (!paymentDefault.isDefault) {
//            throw new RuntimeException("Deveria ser default");
//        }
//
//        if (paymentFallback.isDefault) {
//            throw new RuntimeException("Deveria ser fallback");
//        }
//
//    }
//
//}