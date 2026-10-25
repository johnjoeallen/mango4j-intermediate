package ie.bitstep.mango.utils.masking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaskDefaultsTest {

	@Mask(masker = IdMasker.class)
	public String id;

	@Mask(masker = PanMasker.class)
	public String pan;

	@Mask(masker = FullMasker.class)
	public String full;

	@Mask(masker = CommaSeparatedIdListMasker.class)
	public String ids;

	@Mask(masker = CommaSeparatedPanListMasker.class)
	public String pans;

	@Mask(prefix = 1, postfix = 1)
	public String parameterised;

	@Mask(masker = IdMasker.class, maskingChars = "#")
	public String explicitId;

	@Mask(masker = IdMasker.class, maskingChars = "")
	public String emptyId;

	private static Mask mask(String fieldName) throws NoSuchFieldException {
		return MaskDefaultsTest.class.getField(fieldName).getAnnotation(Mask.class);
	}

	private static Masker masker(String fieldName) throws NoSuchFieldException {
		return new MaskerMaker().make(mask(fieldName));
	}

	@Test
	void maskDefaultIsNotSupplied() throws NoSuchFieldException {
		assertThat(mask("id").maskingChars()).isEmpty();
	}

	@Test
	void idMaskerUsesItsOwnDefaultWhenNotSupplied() throws NoSuchFieldException {
		assertThat(masker("id").mask("1234567890")).isEqualTo(new IdMasker().mask("1234567890")).contains("X");
	}

	@Test
	void panMaskerUsesItsOwnDefaultWhenNotSupplied() throws NoSuchFieldException {
		assertThat(masker("pan").mask("5432234534576562")).isEqualTo(new PanMasker().mask("5432234534576562")).contains("X");
	}

	@Test
	void fullMaskerUsesItsOwnDefaultWhenNotSupplied() throws NoSuchFieldException {
		assertThat(masker("full").mask("secret")).isEqualTo("***");
	}

	@Test
	void commaSeparatedListMaskersUseTheWrappedMaskersDefault() throws NoSuchFieldException {
		assertThat(masker("ids").mask("1234567890,0987654321"))
				.isEqualTo(new CommaSeparatedIdListMasker().mask("1234567890,0987654321"))
				.contains("X");
		assertThat(masker("pans").mask("5432234534576562,5432558257356834"))
				.isEqualTo(new CommaSeparatedPanListMasker().mask("5432234534576562,5432558257356834"))
				.contains("X");
	}

	@Test
	void parameterisedMaskerFallsBackToTheMaskDefault() throws NoSuchFieldException {
		assertThat(masker("parameterised").mask("abcdef")).isEqualTo("a****f");
	}

	@Test
	void explicitMaskingCharsWin() throws NoSuchFieldException {
		assertThat(masker("explicitId").mask("1234567890")).contains("#").doesNotContain("X");
	}

	@Test
	void emptyMaskingCharsCountAsNotSupplied() throws NoSuchFieldException {
		assertThat(masker("emptyId").mask("1234567890")).isEqualTo(masker("id").mask("1234567890"));
	}

	@Test
	void resolveMaskingChars() {
		assertThat(MaskingUtils.resolveMaskingChars("#", "X")).isEqualTo("#");
		assertThat(MaskingUtils.resolveMaskingChars("", "X")).isEqualTo("X");
		assertThat(MaskingUtils.resolveMaskingChars(null, "X")).isEqualTo("X");
		assertThat(MaskingUtils.resolveMaskingChars("", "")).isEqualTo(Mask.DEFAULT_MASKING_CHARS);
		assertThat(MaskingUtils.resolveMaskingChars(null, null)).isEqualTo("*");
		assertThat(MaskingUtils.resolveMaskingChars("", null)).isEqualTo("*");
	}
}
