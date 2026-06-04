# 테스트 규칙

## 프레임워크 & 도구

| 도구 | 용도 |
|------|------|
| JUnit 5 | 테스트 프레임워크 |
| Mockito | 모킹 (단위 테스트) |
| Spring Boot Test | 통합 테스트 |
| `@DataJpaTest` | JPA 레포지토리 테스트 |

## 테스트 레이어 전략

### 1. 단위 테스트 (Unit Test)
대상: `Service`, `Domain`, `ValueObject`

```java
@ExtendWith(MockitoExtension.class)
class AuthWriterServiceTest {

    @InjectMocks
    private AuthWriterService authWriterService;

    @Mock
    private AuthRepository authRepository;

    @Mock
    private TokenProviderPort tokenProviderPort;

    @Mock
    private MemoryMap memoryMap;

    @Test
    void 로그인_성공() {
        // given
        // when
        // then
    }
}
```

### 2. 슬라이스 테스트 (Slice Test)
대상: `Repository`

```java
@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 회원_저장_조회() {
        // given
        Member member = Member.create("닉네임", "oauth123", "KAKAO", "test@test.com", null, null);

        // when
        Member saved = memberRepository.save(member);

        // then
        assertThat(saved.getId()).isNotNull();
    }
}
```

### 3. 통합 테스트 (Integration Test)
대상: Controller (API 엔드포인트)

```java
@SpringBootTest
@AutoConfigureMockMvc
class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 게시글_조회_API() throws Exception {
        mockMvc.perform(get("/v1/posts/1")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }
}
```

## 테스트 네이밍 규칙

- 메서드 이름은 **한국어**로 작성 (가독성 우선)
- 패턴: `{상황}_{결과}` 또는 `{행위}_성공/실패`

```java
@Test
void 존재하지_않는_회원_로그인시_예외_발생()

@Test
void 만료된_토큰으로_요청시_401_반환()

@Test
void 유효한_토큰으로_인증_성공()
```

## Given / When / Then 구조 필수

```java
@Test
void 회원_닉네임_수정_성공() {
    // given
    Member member = Member.create("기존닉네임", ...);
    String newNickname = "새닉네임";

    // when
    member.updateNickname(newNickname);

    // then
    assertThat(member.getNickname()).isEqualTo(newNickname);
}
```

## 도메인 예외 테스트

예외 발생 케이스는 반드시 테스트:

```java
@Test
void 잘못된_이메일_형식_예외_발생() {
    assertThatThrownBy(() -> Email.from("invalid-email"))
            .isInstanceOf(GlobalException.class)
            .extracting(e -> ((GlobalException) e).getErrorType())
            .isEqualTo(GlobalErrorType.EMAIL_INVALID_FORMAT);
}
```

## 테스트 파일 위치

```
src/test/java/backend/daangnbasedbackend/
├── {domain}/
│   ├── domain/
│   │   └── {Domain}Test.java          # 도메인 단위 테스트
│   └── application/
│       └── {Domain}WriterServiceTest.java
└── global/
    └── domain/
        └── EmailTest.java
```

## 현재 테스트 현황

- `DaangnBasedBackendApplicationTests` — 컨텍스트 로드 테스트만 존재
- 실제 비즈니스 로직 테스트는 아직 작성되지 않음 (추가 필요)
