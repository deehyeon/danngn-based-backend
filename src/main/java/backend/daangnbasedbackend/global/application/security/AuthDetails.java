package backend.daangnbasedbackend.global.application.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class AuthDetails implements UserDetails {
    private final Long memberId;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthDetails(Long memberId) {
        this.memberId = memberId;
        this.authorities = Collections.emptyList();
    }

    public AuthDetails(Long memberId, Collection<? extends GrantedAuthority> authorities) {
        this.memberId = memberId;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return String.valueOf(memberId);
    }
}