package com.Maktab101.SpringProject.controler;

import com.Maktab101.SpringProject.dto.users.CustomerResponseDto;
import com.Maktab101.SpringProject.dto.users.PasswordEditDto;
import com.Maktab101.SpringProject.dto.users.RegisterDto;
import com.Maktab101.SpringProject.mapper.MainServicesMapper;
import com.Maktab101.SpringProject.mapper.UserMapper;
import com.Maktab101.SpringProject.model.Customer;
import com.Maktab101.SpringProject.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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

    @PutMapping("/edit/password")
    public ResponseEntity<Void> editPassword(@Valid @RequestBody PasswordEditDto dto) {
        customerService.editPassword(dto.getUserId(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sort")
    public ResponseEntity<List<CustomerResponseDto>> sortCustomers(@RequestBody List<String> sortingFields) {
        List<Customer> sortedCustomers = customerService.sort(sortingFields);
        List<CustomerResponseDto> dtoList = sortedCustomers.stream()
                .map(UserMapper.INSTANCE::toCustomerDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }
}
