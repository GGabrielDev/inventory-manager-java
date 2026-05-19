package com.inventorymanager.backend.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.inventorymanager.backend.audit.AuditService;
import com.inventorymanager.backend.auth.CurrentUser;
import com.inventorymanager.backend.common.ApiException;
import com.inventorymanager.backend.domain.Municipality;
import com.inventorymanager.backend.domain.State;
import com.inventorymanager.backend.domain.Parish;
import com.inventorymanager.backend.repository.BranchRepository;
import com.inventorymanager.backend.repository.DepartmentRepository;
import com.inventorymanager.backend.repository.MunicipalityRepository;
import com.inventorymanager.backend.repository.ParishRepository;
import com.inventorymanager.backend.repository.StateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import java.util.Optional;

public class BranchControllerHostileTest {

    private BranchController controller;
    private StateRepository stateRepo;
    private MunicipalityRepository muniRepo;
    private ParishRepository parishRepo;

    @BeforeEach
    void setUp() {
        stateRepo = mock(StateRepository.class);
        muniRepo = mock(MunicipalityRepository.class);
        parishRepo = mock(ParishRepository.class);
        controller = new BranchController(
            mock(BranchRepository.class), stateRepo, muniRepo, parishRepo,
            mock(DepartmentRepository.class), mock(CurrentUser.class), mock(AuditService.class)
        );
    }

    @Test
    void createRejectsMismatchedMunicipalityAndState() {
        CrudRequest.BranchUpsert request = new CrudRequest.BranchUpsert("branch", "addr", 1L, 2L, 3L);
        
        State s1 = new State(); s1.setId(1L); s1.setName("State 1");
        State s2 = new State(); s2.setId(99L);
        
        Municipality m = new Municipality(); m.setId(2L); m.setName("Muni"); m.setState(s2);
        
        when(stateRepo.findById(1L)).thenReturn(Optional.of(s1));
        when(muniRepo.findById(2L)).thenReturn(Optional.of(m));
        when(parishRepo.findById(3L)).thenReturn(Optional.of(new Parish()));

        ApiException ex = assertThrows(ApiException.class, () -> controller.create(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Municipality Muni does not belong to State State 1", ex.getMessage());
    }

    @Test
    void createRejectsMismatchedParishAndMunicipality() {
        CrudRequest.BranchUpsert request = new CrudRequest.BranchUpsert("branch", "addr", 1L, 2L, 3L);
        
        State s = new State(); s.setId(1L);
        Municipality m1 = new Municipality(); m1.setId(2L); m1.setName("Muni 1"); m1.setState(s);
        Municipality m2 = new Municipality(); m2.setId(99L);
        
        Parish p = new Parish(); p.setId(3L); p.setName("Parish"); p.setMunicipality(m2);
        
        when(stateRepo.findById(1L)).thenReturn(Optional.of(s));
        when(muniRepo.findById(2L)).thenReturn(Optional.of(m1));
        when(parishRepo.findById(3L)).thenReturn(Optional.of(p));

        ApiException ex = assertThrows(ApiException.class, () -> controller.create(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Parish Parish does not belong to Municipality Muni 1", ex.getMessage());
    }
}
