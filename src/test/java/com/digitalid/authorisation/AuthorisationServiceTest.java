package com.digitalid.authorisation;

import com.digitalid.exception.UnauthorisedActionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class AuthorisationServiceTest {

    private AuthorisationService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthorisationService();
    }

    @Test
    void authoriseManagementAction_doesNotThrow_forCentralAuthority() {
        assertDoesNotThrow(() ->
                authService.authoriseManagementAction(OrganisationType.CENTRAL_AUTHORITY)
        );
    }

    @Test
    void authoriseManagementAction_throwsUnauthorisedActionException_forBank() {
        assertThrows(UnauthorisedActionException.class, () ->
                authService.authoriseManagementAction(OrganisationType.BANK)
        );
    }

    @Test
    void authoriseManagementAction_throwsUnauthorisedActionException_forTaxService() {
        assertThrows(UnauthorisedActionException.class, () ->
                authService.authoriseManagementAction(OrganisationType.TAX_SERVICE)
        );
    }

    @Test
    void authoriseManagementAction_throwsUnauthorisedActionException_forDrivingAuthority() {
        assertThrows(UnauthorisedActionException.class, () ->
                authService.authoriseManagementAction(OrganisationType.DRIVING_AUTHORITY)
        );
    }

    @Test
    void authoriseManagementAction_throwsUnauthorisedActionException_forEmployer() {
        assertThrows(UnauthorisedActionException.class, () ->
                authService.authoriseManagementAction(OrganisationType.EMPLOYER)
        );
    }

    @ParameterizedTest(name = "{0} must not be authorised for management actions")
    @EnumSource(value = OrganisationType.class, names = {"CENTRAL_AUTHORITY"}, mode = EnumSource.Mode.EXCLUDE)
    void authoriseManagementAction_throwsUnauthorisedActionException_forAllConsumingOrganisations(
            OrganisationType orgType) {

        assertThrows(UnauthorisedActionException.class, () ->
                        authService.authoriseManagementAction(orgType),
                orgType + " should not be authorised for management actions"
        );
    }

    @Test
    void authoriseManagementAction_exceptionMessage_identifiesTheRejectedOrgType() {
        UnauthorisedActionException thrown = assertThrows(
                UnauthorisedActionException.class,
                () -> authService.authoriseManagementAction(OrganisationType.BANK)
        );

        assertTrue(thrown.getMessage().contains("BANK"),
                "Exception message should identify the rejected organisation type");
    }

    @ParameterizedTest(name = "{0} must be authorised for consumption actions")
    @EnumSource(OrganisationType.class)
    void authoriseConsumptionAction_doesNotThrow_forAnyOrganisationType(
            OrganisationType orgType) {

        assertDoesNotThrow(() ->
                        authService.authoriseConsumptionAction(orgType),
                orgType + " should be authorised for consumption actions"
        );
    }
}