package com.iris.common.lib.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataMaskerTest {

    // ── maskPhone ─────────────────────────────────────────────────────────────

    @Nested
    class MaskPhone {

        @Test
        void givenE164PhoneWithPlus_whenMaskPhone_returnPrefixAndLastTwoDigitsMasked() {
            assertEquals("+254****78", DataMasker.maskPhone("+254712345678"));
        }

        @Test
        void givenPhoneWithoutPlus_whenMaskPhone_returnPrefixAndLastTwoDigitsMasked() {
            assertEquals("254****78", DataMasker.maskPhone("254712345678"));
        }

        @Test
        void givenShortPhoneUnderSixChars_whenMaskPhone_returnFullyMasked() {
            assertEquals("****", DataMasker.maskPhone("12345"));
        }

        @Test
        void givenPhoneAtBoundaryPrefixPlusTwoChars_whenMaskPhone_returnPrefixMasked() {
            // "+254" (4) + "12" (2) = 6 chars total — no suffix to show
            assertEquals("+254****", DataMasker.maskPhone("+25412"));
        }

        @Test
        void givenNullPhone_whenMaskPhone_returnFullyMasked() {
            assertEquals("****", DataMasker.maskPhone(null));
        }
    }

    // ── maskEmail ────────────────────────────────────────────────────────────

    @Nested
    class MaskEmail {

        @Test
        void givenValidEmail_whenMaskEmail_returnFirstCharAndDomainOnly() {
            assertEquals("e****@example.com", DataMasker.maskEmail("elvis@example.com"));
        }

        @Test
        void givenEmailWithoutAtSign_whenMaskEmail_returnFullyMasked() {
            assertEquals("****", DataMasker.maskEmail("notanemail"));
        }

        @Test
        void givenNullEmail_whenMaskEmail_returnFullyMasked() {
            assertEquals("****", DataMasker.maskEmail(null));
        }

        @Test
        void givenSingleCharBeforeAt_whenMaskEmail_returnMaskedCorrectly() {
            assertEquals("a****@domain.io", DataMasker.maskEmail("a@domain.io"));
        }
    }

    // ── maskToken ────────────────────────────────────────────────────────────

    @Nested
    class MaskToken {

        @Test
        void givenLongToken_whenMaskToken_returnFirstSixCharsWithSuffix() {
            assertEquals("eyJhbG...[masked]", DataMasker.maskToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"));
        }

        @Test
        void givenShortTokenUnderSixChars_whenMaskToken_returnFullyMasked() {
            assertEquals("****", DataMasker.maskToken("abc"));
        }

        @Test
        void givenNullToken_whenMaskToken_returnFullyMasked() {
            assertEquals("****", DataMasker.maskToken(null));
        }

        @Test
        void givenTokenExactlySixChars_whenMaskToken_returnFirstSixWithSuffix() {
            assertEquals("abcdef...[masked]", DataMasker.maskToken("abcdef"));
        }
    }

    // ── maskName ─────────────────────────────────────────────────────────────

    @Nested
    class MaskName {

        @Test
        void givenFullName_whenMaskName_returnFirstCharAndAsterisks() {
            assertEquals("E****", DataMasker.maskName("Elvis"));
        }

        @Test
        void givenEmptyName_whenMaskName_returnFullyMasked() {
            assertEquals("****", DataMasker.maskName(""));
        }

        @Test
        void givenNullName_whenMaskName_returnFullyMasked() {
            assertEquals("****", DataMasker.maskName(null));
        }

        @Test
        void givenSingleCharName_whenMaskName_returnCharAndAsterisks() {
            assertEquals("J****", DataMasker.maskName("J"));
        }
    }

    // ── maskRaw ──────────────────────────────────────────────────────────────

    @Nested
    class MaskRaw {

        private static final String SAMPLE_JWT =
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjMifQ.SflKxwRJSMeKKF2QT4fwpMeJf36P";

        @Test
        void givenStringContainingJwt_whenMaskRaw_returnJwtMasked() {
            String input = "Bearer " + SAMPLE_JWT;
            String result = DataMasker.maskRaw(input);
            assertTrue(result.contains("...[masked]"));
            assertFalse(result.contains(SAMPLE_JWT));
        }

        @Test
        void givenStringContainingEmail_whenMaskRaw_returnEmailMasked() {
            String result = DataMasker.maskRaw("Contact us at support@company.com");
            assertTrue(result.contains("s****@company.com"));
            assertFalse(result.contains("support@company.com"));
        }

        @Test
        void givenStringWithBothJwtAndEmail_whenMaskRaw_returnBothMasked() {
            String input = "Token=" + SAMPLE_JWT + " email=user@test.com";
            String result = DataMasker.maskRaw(input);
            assertTrue(result.contains("...[masked]"));
            assertTrue(result.contains("u****@test.com"));
        }

        @Test
        void givenStringWithNoSensitiveData_whenMaskRaw_returnUnchanged() {
            String input = "Hello world, status=OK";
            assertEquals(input, DataMasker.maskRaw(input));
        }

        @Test
        void givenNull_whenMaskRaw_returnNull() {
            assertNull(DataMasker.maskRaw(null));
        }
    }

    // ── maskJson — field-name rules ───────────────────────────────────────────

    @Nested
    class MaskJsonFieldRules {

        @Test
        void givenJsonWithOtpField_whenMaskJson_returnFullyMasked() {
            String result = DataMasker.maskJson("{\"otp\":\"123456\"}");
            assertEquals("{\"otp\":\"****\"}", result);
        }

        @Test
        void givenJsonWithPasswordField_whenMaskJson_returnFullyMasked() {
            String result = DataMasker.maskJson("{\"password\":\"s3cr3t!\"}");
            assertEquals("{\"password\":\"****\"}", result);
        }

        @Test
        void givenJsonWithPinField_whenMaskJson_returnFullyMasked() {
            String result = DataMasker.maskJson("{\"pin\":\"4321\"}");
            assertEquals("{\"pin\":\"****\"}", result);
        }

        @Test
        void givenJsonWithSecretField_whenMaskJson_returnFullyMasked() {
            String result = DataMasker.maskJson("{\"secret\":\"mysecret\"}");
            assertEquals("{\"secret\":\"****\"}", result);
        }

        @Test
        void givenJsonWithAuthTokenField_whenMaskJson_returnFullyMasked() {
            String result = DataMasker.maskJson("{\"auth_token\":\"abc123\"}");
            assertEquals("{\"auth_token\":\"****\"}", result);
        }

        @Test
        void givenJsonWithTokenField_whenMaskJson_returnTokenMasked() {
            String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload.sig";
            String result = DataMasker.maskJson("{\"token\":\"" + jwt + "\"}");
            assertTrue(result.contains("...[masked]"));
            assertFalse(result.contains(jwt));
        }

        @Test
        void givenJsonWithAccessTokenField_whenMaskJson_returnTokenMasked() {
            String result = DataMasker.maskJson("{\"accessToken\":\"eyJhbGciOi...\"}");
            assertTrue(result.contains("...[masked]"));
        }

        @Test
        void givenJsonWithAuthorizationField_whenMaskJson_returnTokenMasked() {
            String result = DataMasker.maskJson("{\"authorization\":\"Bearer eyJhbGciOi...\"}");
            assertTrue(result.contains("...[masked]"));
        }

        @Test
        void givenJsonWithAccountSidField_whenMaskJson_returnTokenMasked() {
            String result = DataMasker.maskJson("{\"account_sid\":\"ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"}");
            assertTrue(result.contains("...[masked]"));
        }

        @Test
        void givenJsonWithPhoneNumberField_whenMaskJson_returnPhoneMasked() {
            String result = DataMasker.maskJson("{\"phoneNumber\":\"+254712345678\"}");
            assertEquals("{\"phoneNumber\":\"+254****78\"}", result);
        }

        @Test
        void givenJsonWithToField_whenMaskJson_returnPhoneMasked() {
            String result = DataMasker.maskJson("{\"to\":\"+254712345678\"}");
            assertEquals("{\"to\":\"+254****78\"}", result);
        }

        @Test
        void givenJsonWithMsisdnField_whenMaskJson_returnPhoneMasked() {
            String result = DataMasker.maskJson("{\"msisdn\":\"254712345678\"}");
            assertEquals("{\"msisdn\":\"254****78\"}", result);
        }

        @Test
        void givenJsonWithEmailField_whenMaskJson_returnEmailMasked() {
            String result = DataMasker.maskJson("{\"email\":\"user@example.com\"}");
            assertEquals("{\"email\":\"u****@example.com\"}", result);
        }

        @Test
        void givenJsonWithNameField_whenMaskJson_returnNameMasked() {
            String result = DataMasker.maskJson("{\"name\":\"Alice\"}");
            assertEquals("{\"name\":\"A****\"}", result);
        }

        @Test
        void givenJsonWithBusinessNameField_whenMaskJson_returnNameMasked() {
            String result = DataMasker.maskJson("{\"businessName\":\"Acme Corp\"}");
            assertEquals("{\"businessName\":\"A****\"}", result);
        }

        @Test
        void givenJsonWithNonSensitiveField_whenMaskJson_returnValueUnchanged() {
            String result = DataMasker.maskJson("{\"status\":\"ACTIVE\",\"responseCode\":200}");
            assertEquals("{\"status\":\"ACTIVE\",\"responseCode\":200}", result);
        }
    }

    // ── maskJson — value-level JWT detection ──────────────────────────────────

    @Nested
    class MaskJsonJwtValueDetection {

        @Test
        void givenJsonWithJwtInArbitraryTextField_whenMaskJson_returnValueMasked() {
            String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjMifQ.SflKxwRJSMeKKF2QT4fwpM";
            String result = DataMasker.maskJson("{\"data\":\"" + jwt + "\"}");
            assertTrue(result.contains("...[masked]"));
            assertFalse(result.contains(jwt));
        }
    }

    // ── maskJson — structural cases ───────────────────────────────────────────

    @Nested
    class MaskJsonStructure {

        @Test
        void givenNestedObject_whenMaskJson_returnSensitiveFieldsInNestedObjectMasked() {
            String json = "{\"profile\":{\"email\":\"u@test.com\",\"status\":\"ACTIVE\"}}";
            String result = DataMasker.maskJson(json);
            assertTrue(result.contains("u****@test.com"));
            assertTrue(result.contains("ACTIVE"));
        }

        @Test
        void givenArrayOfObjects_whenMaskJson_returnEachElementMasked() {
            String json = "[{\"phoneNumber\":\"+254700000001\"},{\"phoneNumber\":\"+254700000002\"}]";
            String result = DataMasker.maskJson(json);
            assertTrue(result.contains("+254****01"));
            assertTrue(result.contains("+254****02"));
            assertFalse(result.contains("+254700000001"));
            assertFalse(result.contains("+254700000002"));
        }

        @Test
        void givenDeeplyNestedSensitiveFields_whenMaskJson_returnAllSensitiveFieldsMasked() {
            String json = "{\"data\":{\"user\":{\"email\":\"a@b.com\",\"token\":\"eyJhbGci.payload.sig\"}}}";
            String result = DataMasker.maskJson(json);
            assertTrue(result.contains("a****@b.com"));
            assertTrue(result.contains("...[masked]"));
        }

        @Test
        void givenMixedSensitiveAndNonSensitiveFields_whenMaskJson_returnOnlySensitiveMasked() {
            String json = "{\"userId\":\"uuid-1234\",\"email\":\"bob@test.com\",\"userType\":\"CUSTOMER\"}";
            String result = DataMasker.maskJson(json);
            assertTrue(result.contains("uuid-1234"));
            assertTrue(result.contains("b****@test.com"));
            assertTrue(result.contains("CUSTOMER"));
            assertFalse(result.contains("bob@test.com"));
        }

        @Test
        void givenInvalidJson_whenMaskJson_returnRawMaskedFallback() {
            String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjMifQ.SflKxwRJSMeKKF2QT4fwpM";
            String result = DataMasker.maskJson("not-json containing " + jwt);
            assertTrue(result.contains("...[masked]"));
            assertFalse(result.contains(jwt));
        }

        @Test
        void givenNullJson_whenMaskJson_returnNull() {
            assertNull(DataMasker.maskJson(null));
        }

        @Test
        void givenBlankJson_whenMaskJson_returnBlank() {
            assertEquals("   ", DataMasker.maskJson("   "));
        }
    }

    // ── maskObject ────────────────────────────────────────────────────────────

    @Nested
    class MaskObject {

        record UserProfile(String userId, String name, String email, String phoneNumber, String token) {}

        @Test
        void givenPojoWithSensitiveFields_whenMaskObject_returnAllSensitiveFieldsMasked() {
            String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjMifQ.SflKxwRJSMeKKF2QT4fwpM";
            UserProfile profile = new UserProfile("uuid-abc", "Alice", "alice@mail.com", "+254712345678", jwt);

            String result = DataMasker.maskObject(profile);

            assertTrue(result.contains("uuid-abc"),        "userId should not be masked");
            assertFalse(result.contains("Alice"),          "name should be masked");
            assertFalse(result.contains("alice@mail.com"), "email should be masked");
            assertFalse(result.contains("+254712345678"),  "phoneNumber should be masked");
            assertFalse(result.contains(jwt),              "token should be masked");
        }

        @Test
        void givenNull_whenMaskObject_returnNullString() {
            assertEquals("null", DataMasker.maskObject(null));
        }

        @Test
        void givenUnserializableObject_whenMaskObject_returnPlaceholder() {
            // Anonymous class with a self-reference cannot be serialized
            Object unserializable = new Object() {
                public Object self = this;
            };
            assertEquals("[unserializable]", DataMasker.maskObject(unserializable));
        }

        @Test
        void givenPojoWithNoSensitiveFields_whenMaskObject_returnUnchangedValues() {
            record SafeDto(String status, int code) {}
            String result = DataMasker.maskObject(new SafeDto("OK", 200));
            assertTrue(result.contains("OK"));
            assertTrue(result.contains("200"));
        }
    }
}
