package backend.daangnbasedbackend.global.domain;

import backend.daangnbasedbackend.global.exception.common.GlobalException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "user@example.com",
            "user.name+tag@example.co.kr",
            "user_123@sub.domain.com",
            "a@b.io"
    })
    @DisplayName("올바른 형식의 이메일은 생성에 성공한다")
    void validFormat_createsEmail(String address) {
        Email email = Email.from(address);

        assertThat(email.address()).isEqualTo(address);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not-an-email",
            "missing@domain",
            "@no-local.com",
            "spaces in@email.com",
            ""
    })
    @DisplayName("잘못된 형식의 이메일은 GlobalException을 던진다")
    void invalidFormat_throwsGlobalException(String address) {
        assertThatThrownBy(() -> Email.from(address))
                .isInstanceOf(GlobalException.class);
    }

    @Test
    @DisplayName("동일한 주소를 가진 Email은 값이 같다")
    void sameAddress_equalValues() {
        Email email1 = Email.from("same@example.com");
        Email email2 = Email.from("same@example.com");

        assertThat(email1).isEqualTo(email2);
        assertThat(email1.address()).isEqualTo(email2.address());
    }
}
