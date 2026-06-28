package com.insurance.claims.config;

import com.insurance.claims.domain.Customer;
import com.insurance.claims.domain.Policy;
import com.insurance.claims.domain.PolicyStatus;
import com.insurance.claims.domain.PolicyType;
import com.insurance.claims.domain.SystemUser;
import com.insurance.claims.repository.CustomerRepository;
import com.insurance.claims.repository.PolicyRepository;
import com.insurance.claims.repository.SystemUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Configuration
@Profile({"sqlite", "sqlserver"})
public class DataSeeder {
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Bean
    CommandLineRunner seedExistingPolicyData(CustomerRepository customerRepository,
                                             PolicyRepository policyRepository,
                                             SystemUserRepository systemUserRepository) {
        return args -> {
            seedSystemAdministrator(systemUserRepository);

            Customer autoCustomer = customerRepository.findByEmail("budi.santoso@example.com")
                    .orElseGet(() -> customerRepository.save(createCustomer(
                    "Budi Santoso",
                    "budi.santoso@example.com",
                    "+62 812 1000 0001",
                    "Jakarta Selatan"
            )));
            Customer homeCustomer = customerRepository.findByEmail("siti.rahma@example.com")
                    .orElseGet(() -> customerRepository.save(createCustomer(
                    "Siti Rahma",
                    "siti.rahma@example.com",
                    "+62 812 1000 0002",
                    "Bandung"
            )));
            Customer healthCustomer = customerRepository.findByEmail("andi.wijaya@example.com")
                    .orElseGet(() -> customerRepository.save(createCustomer(
                    "Andi Wijaya",
                    "andi.wijaya@example.com",
                    "+62 812 1000 0003",
                    "Surabaya"
            )));

            seedPolicyIfMissing(policyRepository, autoCustomer, "POL-AUTO-0001", PolicyType.AUTO, "250000000");
            seedPolicyIfMissing(policyRepository, homeCustomer, "POL-HOME-0001", PolicyType.HOME, "750000000");
            seedPolicyIfMissing(policyRepository, healthCustomer, "POL-HEALTH-0001", PolicyType.HEALTH, "100000000");
        };
    }

    private void seedSystemAdministrator(SystemUserRepository systemUserRepository) {
        if (systemUserRepository.existsByUsername("jdoe")) {
            systemUserRepository.findByUsername("jdoe").ifPresent(user -> {
                if (!isBCryptHash(user.getPasswordHash())) {
                    user.setPasswordHash(passwordEncoder.encode("admin123"));
                }
                user.setFullName("John Doe");
                user.setRole("System Administrator");
                user.setPermissions("CLAIMS_VIEW,CLAIMS_CREATE,CLAIMS_EDIT,CLAIMS_STATUS,CLAIMS_NOTES,USERS_MANAGE");
                systemUserRepository.save(user);
            });
            return;
        }
        SystemUser user = new SystemUser();
        user.setFullName("John Doe");
        user.setUsername("jdoe");
        user.setPasswordHash(passwordEncoder.encode("admin123"));
        user.setRole("System Administrator");
        user.setPermissions("CLAIMS_VIEW,CLAIMS_CREATE,CLAIMS_EDIT,CLAIMS_STATUS,CLAIMS_NOTES,USERS_MANAGE");
        systemUserRepository.save(user);
    }

    private boolean isBCryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    private void seedPolicyIfMissing(PolicyRepository policyRepository,
                                     Customer customer,
                                     String policyNumber,
                                     PolicyType type,
                                     String coverageAmount) {
        if (policyRepository.existsByPolicyNumber(policyNumber)) {
            return;
        }
        policyRepository.save(createPolicy(
                customer,
                policyNumber,
                type,
                new BigDecimal(coverageAmount),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        ));
    }

    private Customer createCustomer(String fullName, String email, String phone, String address) {
        Customer customer = new Customer();
        customer.setFullName(fullName);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setAddress(address);
        return customer;
    }

    private Policy createPolicy(Customer customer,
                                String policyNumber,
                                PolicyType type,
                                BigDecimal coverageAmount,
                                LocalDate startDate,
                                LocalDate endDate) {
        Policy policy = new Policy();
        policy.setCustomer(customer);
        policy.setPolicyNumber(policyNumber);
        policy.setType(type);
        policy.setCoverageAmount(coverageAmount);
        policy.setStartDate(startDate);
        policy.setEndDate(endDate);
        policy.setStatus(PolicyStatus.ACTIVE);
        return policy;
    }
}
