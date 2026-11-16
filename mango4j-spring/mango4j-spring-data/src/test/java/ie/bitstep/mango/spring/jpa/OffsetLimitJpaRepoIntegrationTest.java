package ie.bitstep.mango.spring.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ContextConfiguration(classes = OffsetLimitJpaRepoIntegrationTest.Config.class)
@TestPropertySource(properties = {
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.jpa.show-sql=true",
	"spring.jpa.properties.hibernate.format_sql=true",
	"spring.jpa.properties.hibernate.highlight_sql=true"
})
class OffsetLimitJpaRepoIntegrationTest {

	// ===== Entity =====
	@Entity
	@Table(name = "users")
	public static class User {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Column(nullable = false)
		private String email;

		@Column(nullable = false)
		private String name;

		protected User() {
		}

		public User(String email, String name) {
			this.email = email;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getEmail() {
			return email;
		}

		public String getName() {
			return name;
		}
	}

	// ===== Repository =====
	interface UserRepo extends JpaRepository<User, Long> {
		Page<User> findAllByOrderByIdAsc(Pageable pageable);
	}

	// ===== Minimal Boot config for nested repo/entity discovery =====
	@SpringBootConfiguration
	@EnableJpaRepositories(
		considerNestedRepositories = true,
		basePackageClasses = OffsetLimitJpaRepoIntegrationTest.class)
	@EntityScan(basePackageClasses = OffsetLimitJpaRepoIntegrationTest.class)
	static class Config {
		// no beans needed; @DataJpaTest autoconfigures H2 + JPA
	}

	@Autowired
	private UserRepo repo;

	@BeforeEach
	void seed() {
		repo.deleteAll();
		for (int i = 1; i <= 50; i++) {
			repo.save(new User("user%02d@example.com".formatted(i), "User %02d".formatted(i)));
		}
		assertEquals(50, repo.count());
	}

	// ===== Tests (must use factory method) =====

	@Test
	void pageableValid() {
		OffsetLimitPageable p = (OffsetLimitPageable) OffsetLimitPageable.of(25, 10);

		assertThat(p).isNotNull();
		assertThat(p.getOffset()).isEqualTo(25);
		assertThat(p.getLimit()).isEqualTo(10);
		assertThat(p.getPageNumber()).isEqualTo(2);
		assertThat(p.getSort()).isEqualTo(Sort.unsorted());
	}

	@Test
	void pageableInvalidOffset() {
		IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () ->
			OffsetLimitPageable.of(-1, 10)
		);

		assertThat(illegalArgumentException.getMessage()).isEqualTo("offset must be >= 0");
	}

	@Test
	void pageableInvalidLimit() {
		IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () ->
			OffsetLimitPageable.of(0, 0)
		);

		assertThat(illegalArgumentException.getMessage()).isEqualTo("limit must be > 0");
	}

	@Test
	void pageableSorted() {
		Pageable p = OffsetLimitPageable.of(25, 10, Sort.by("id"));
		assertThat(p.getSort()).isEqualTo(Sort.by("id"));
	}

	@Test
	void pageableUnsorted() {
		Pageable p = OffsetLimitPageable.of(25, 10, Sort.unsorted());
		assertThat(p.getSort()).isEqualTo(Sort.unsorted());
	}

	@Test
	void pageableNext() {
		OffsetLimitPageable p = (OffsetLimitPageable) OffsetLimitPageable.of(25, 10);
		Pageable next = p.next();

		assertThat(next.getOffset()).isEqualTo(35);
	}

	@Test
	void pageablePrevious() {
		OffsetLimitPageable p = (OffsetLimitPageable) OffsetLimitPageable.of(25, 10);
		Pageable previous = p.previousOrFirst();

		assertThat(previous.getOffset()).isEqualTo(15);
	}

	@Test
	void pageablePreviousOrFirst() {
		OffsetLimitPageable p = (OffsetLimitPageable) OffsetLimitPageable.of(15, 10);
		Pageable previous = p.previousOrFirst();

		assertThat(previous.getOffset()).isEqualTo(5);
	}

	@Test
	void pageableFirst() {
		OffsetLimitPageable p = (OffsetLimitPageable) OffsetLimitPageable.of(25, 10);
		Pageable previous = p.first();

		assertThat(previous.getOffset()).isZero();
	}

	@Test
	void hasPrevious() {
		assertThat(OffsetLimitPageable.of(10, 10).withPage(2).getPageNumber()).isEqualTo(2);
	}

	@Test
	void withPage() {
		assertThat(OffsetLimitPageable.of(0, 10).hasPrevious()).isFalse();
		assertThat(OffsetLimitPageable.of(1, 10).hasPrevious()).isTrue();
	}

	@Test
	void offset5_limit10_returns_users_6_to_15() {
		Pageable p = OffsetLimitPageable.of(5, 10);
		Page<User> page = repo.findAllByOrderByIdAsc(p);

		assertEquals(10, page.getNumberOfElements());
		assertEquals("User 06", page.getContent().get(0).getName());
		assertEquals("User 15", page.getContent().get(9).getName());
	}

	@Test
	void offset0_limit3_returns_users_1_to_3() {
		Pageable p = OffsetLimitPageable.of(0, 3);
		Page<User> page = repo.findAllByOrderByIdAsc(p);
		assertEquals(3, page.getNumberOfElements());
		assertEquals("User 01", page.getContent().get(0).getName());
		assertEquals("User 03", page.getContent().get(2).getName());
	}

	@Test
	void offset49_limit10_returns_only_user_50() {
		Pageable p = OffsetLimitPageable.of(49, 10);
		Page<User> page = repo.findAllByOrderByIdAsc(p);
		assertEquals(1, page.getNumberOfElements());
		assertEquals("User 50", page.getContent().get(0).getName());
	}

	@Test
	void large_offset_returns_empty() {
		Pageable p = OffsetLimitPageable.of(1000, 10);
		Page<User> page = repo.findAllByOrderByIdAsc(p);
		assertTrue(page.isEmpty());
	}
}
