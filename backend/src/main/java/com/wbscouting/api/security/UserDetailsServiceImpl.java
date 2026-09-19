package com.wbscouting.api.security;

import com.wbscouting.api.entity.Admin;
import com.wbscouting.api.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByEmail(email)
                .filter(Admin::getIsActive)
                .orElseThrow(() -> new UsernameNotFoundException("Administrador não encontrado ou inativo com o email: " + email));

        return new User(
                admin.getEmail(),
                admin.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + admin.getRole().name()))
        );
    }
}
