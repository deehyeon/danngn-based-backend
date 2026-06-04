package backend.daangnbasedbackend.global.application.security;

import backend.daangnbasedbackend.auth.application.required.AuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthDetailsService implements UserDetailsService {
    private final AuthRepository authRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Long memberId = Long.valueOf(username);

        boolean exists = authRepository.existsById(memberId);
        if (!exists) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다");
        }
        return new AuthDetails(memberId);
    }
}