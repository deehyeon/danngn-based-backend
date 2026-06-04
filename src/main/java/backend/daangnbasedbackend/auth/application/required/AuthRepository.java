package backend.daangnbasedbackend.auth.application.required;

import backend.daangnbasedbackend.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepository extends JpaRepository<Member, Long> {}
