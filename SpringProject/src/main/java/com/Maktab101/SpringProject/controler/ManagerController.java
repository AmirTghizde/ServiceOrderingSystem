package com.Maktab101.SpringProject.controler;

import com.Maktab101.SpringProject.dto.ManagerResponseDto;
import com.Maktab101.SpringProject.dto.RegisterDto;
import com.Maktab101.SpringProject.mapper.UserMapper;
import com.Maktab101.SpringProject.model.Manager;
import com.Maktab101.SpringProject.service.CustomerService;
import com.Maktab101.SpringProject.service.ManagerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager")
public class ManagerController {

    private final ManagerService managerService;

    @Autowired
    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @PostMapping
    public ResponseEntity<ManagerResponseDto> registerManager(@Valid @RequestBody RegisterDto registerDto) {
        Manager manager = managerService.register(registerDto);
        ManagerResponseDto managerDto = UserMapper.INSTANCE.toManagerDto(manager);
        return ResponseEntity.status(HttpStatus.CREATED).body(managerDto);
    }
}