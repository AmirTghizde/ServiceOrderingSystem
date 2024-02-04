package com.Maktab101.SpringProject.controler;

import com.Maktab101.SpringProject.dto.CustomerResponseDto;
import com.Maktab101.SpringProject.dto.RegisterDto;
import com.Maktab101.SpringProject.dto.services.ServiceNameDto;
import com.Maktab101.SpringProject.mapper.UserMapper;
import com.Maktab101.SpringProject.model.Customer;
import com.Maktab101.SpringProject.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponseDto> registerCustomer(@Valid @RequestBody RegisterDto registerDto) {
        Customer customer = customerService.register(registerDto);
        CustomerResponseDto customerDto = UserMapper.INSTANCE.toCustomerDto(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(customerDto);
    }
}