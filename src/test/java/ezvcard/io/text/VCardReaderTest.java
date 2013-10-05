package ezvcard.io.text;

import static ezvcard.util.TestUtils.assertIntEquals;
import static ezvcard.util.TestUtils.assertSetEquals;
import static ezvcard.util.TestUtils.assertValidate;
import static ezvcard.util.TestUtils.assertWarnings;
import static ezvcard.util.VCardStringUtils.NEWLINE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;

import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.io.LuckyNumType;
import ezvcard.io.LuckyNumType.LuckyNumScribe;
import ezvcard.io.MyFormattedNameType;
import ezvcard.io.MyFormattedNameType.MyFormattedNameScribe;
import ezvcard.io.SkipMeException;
import ezvcard.parameter.AddressTypeParameter;
import ezvcard.parameter.EmailTypeParameter;
import ezvcard.parameter.ImageTypeParameter;
import ezvcard.parameter.KeyTypeParameter;
import ezvcard.parameter.TelephoneTypeParameter;
import ezvcard.property.AddressType;
import ezvcard.property.BirthdayType;
import ezvcard.property.CategoriesType;
import ezvcard.property.ClassificationType;
import ezvcard.property.EmailType;
import ezvcard.property.FbUrlType;
import ezvcard.property.FormattedNameType;
import ezvcard.property.GeoType;
import ezvcard.property.KeyType;
import ezvcard.property.LabelType;
import ezvcard.property.LanguageType;
import ezvcard.property.MailerType;
import ezvcard.property.NicknameType;
import ezvcard.property.NoteType;
import ezvcard.property.OrganizationType;
import ezvcard.property.PhotoType;
import ezvcard.property.ProdIdType;
import ezvcard.property.ProfileType;
import ezvcard.property.RawType;
import ezvcard.property.RevisionType;
import ezvcard.property.RoleType;
import ezvcard.property.SortStringType;
import ezvcard.property.SourceDisplayTextType;
import ezvcard.property.SourceType;
import ezvcard.property.StructuredNameType;
import ezvcard.property.TelephoneType;
import ezvcard.property.TimezoneType;
import ezvcard.property.TitleType;
import ezvcard.property.UidType;
import ezvcard.property.UrlType;
import ezvcard.property.VCardType;
import ezvcard.util.PartialDate;
import ezvcard.util.TelUri;
import ezvcard.util.UtcOffset;

/*
 Copyright (c) 2013, Michael Angstadt
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met: 

 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer. 
 2. Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution. 

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 The views and conclusions contained in the software and documentation are those
 of the authors and should not be interpreted as representing official policies, 
 either expressed or implied, of the FreeBSD Project.
 */

/**
 * @author Michael Angstadt
 */
@SuppressWarnings("resource")
public class VCardReaderTest {
	/**
	 * Tests to make sure it can read sub types properly
	 */
	@Test
	public void getSubTypes() throws Throwable {
		//@formatter:off
		String str =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"NOTE;x-size=8:The note\r\n" +
		"ADR;HOME;WORK:;;;;\r\n" + //nameless parameters
		"LABEL;type=dOm;TyPE=parcel:\r\n" + //repeated parameter name
		"END:VCARD\r\n";
		//@formatter:on

		VCardReader reader = new VCardReader(str);
		VCard vcard = reader.readNext();

		NoteType note = vcard.getNotes().get(0);
		assertEquals(1, note.getSubTypes().size());
		assertEquals("8", note.getSubTypes().first("X-SIZE"));
		assertEquals("8", note.getSubTypes().first("x-size"));
		assertNull(note.getSubTypes().first("non-existant"));

		AddressType adr = vcard.getAddresses().get(0);
		assertEquals(2, adr.getSubTypes().size());
		assertSetEquals(adr.getTypes(), AddressTypeParameter.HOME, AddressTypeParameter.WORK);

		LabelType label = vcard.getOrphanedLabels().get(0);
		assertEquals(2, label.getSubTypes().size());
		assertSetEquals(label.getTypes(), AddressTypeParameter.DOM, AddressTypeParameter.PARCEL);

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void type_parameter_enclosed_in_double_quotes() throws Throwable {
		//@formatter:off
		String str =
		"BEGIN:VCARD\r\n" +
		"VERSION:4.0\r\n" +
		"ADR;TYPE=\"dom,home,work\":;;;;\r\n" +
		"ADR;TYPE=\"dom\",\"home\",\"work\":;;;;\r\n" +
		"ADR;TYPE=\"dom\",home,work:;;;;\r\n" +
		"ADR;TYPE=dom,home,work:;;;;\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		VCardReader reader = new VCardReader(str);
		VCard vcard = reader.readNext();

		AddressType adr = vcard.getAddresses().get(0);
		assertEquals(3, adr.getSubTypes().size());
		assertSetEquals(adr.getTypes(), AddressTypeParameter.DOM, AddressTypeParameter.HOME, AddressTypeParameter.WORK);

		adr = vcard.getAddresses().get(1);
		assertEquals(3, adr.getSubTypes().size());
		assertSetEquals(adr.getTypes(), AddressTypeParameter.DOM, AddressTypeParameter.HOME, AddressTypeParameter.WORK);

		adr = vcard.getAddresses().get(2);
		assertEquals(3, adr.getSubTypes().size());
		assertSetEquals(adr.getTypes(), AddressTypeParameter.DOM, AddressTypeParameter.HOME, AddressTypeParameter.WORK);

		adr = vcard.getAddresses().get(3);
		assertEquals(3, adr.getSubTypes().size());
		assertSetEquals(adr.getTypes(), AddressTypeParameter.DOM, AddressTypeParameter.HOME, AddressTypeParameter.WORK);

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	/**
	 * All "quoted-printable" values should be decoded by the VCardReader.
	 */
	@Test
	public void decodeQuotedPrintable() throws Throwable {
		//@formatter:off
		String str =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"LABEL;HOME;ENCODING=QUOTED-PRINTABLE:123 Main St.=0D=0A\r\n" +
		" Austin, TX 91827=0D=0A\r\n" +
		" USA\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		VCardReader reader = new VCardReader(str);
		VCard vcard = reader.readNext();

		LabelType label = vcard.getOrphanedLabels().get(0);
		assertEquals("123 Main St.\r\nAustin, TX 91827\r\nUSA", label.getValue());
		assertNull(label.getSubTypes().getEncoding()); //ENCODING sub type should be removed

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	/**
	 * Tests to make sure it can unfold folded lines.
	 */
	@Test
	public void unfold() throws Throwable {
		//@formatter:off
		String str =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"NOTE:The vCard MIME Directory Profile also provides support for represent\r\n" +
		" ing other important information about the person associated with the dire\r\n" +
		" ctory entry. For instance, the date of birth of the person\\; an audio clip \r\n" +
		" describing the pronunciation of the name associated with the directory en\r\n" +
		" try, or some other application of the digital sound\\; longitude and latitu\r\n" +
		" de geo-positioning information related to the person associated with the \r\n" +
		" directory entry\\; date and time that the directory information was last up\r\n" +
		" dated\\; annotations often written on a business card\\; Uniform Resource Loc\r\n" +
		" ators (URL) for a website\\; public key information.\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		VCardReader reader = new VCardReader(str);
		VCard vcard = reader.readNext();

		String expected = "The vCard MIME Directory Profile also provides support for representing other important information about the person associated with the directory entry. For instance, the date of birth of the person; an audio clip describing the pronunciation of the name associated with the directory entry, or some other application of the digital sound; longitude and latitude geo-positioning information related to the person associated with the directory entry; date and time that the directory information was last updated; annotations often written on a business card; Uniform Resource Locators (URL) for a website; public key information.";
		String actual = vcard.getNotes().get(0).getValue();
		assertEquals(expected, actual);

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	/**
	 * Tests to make sure it can read extended types.
	 */
	@Test
	public void readExtendedType() throws Throwable {
		//@formatter:off
		String str =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"X-LUCKY-NUM:24\r\n" +
		"X-GENDER:ma\\,le\r\n" +
		"X-LUCKY-NUM:22\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		VCardReader reader = new VCardReader(str);
		reader.registerScribe(new LuckyNumScribe());
		VCard vcard = reader.readNext();

		//read a type that has a type class
		List<LuckyNumType> luckyNumTypes = vcard.getTypes(LuckyNumType.class);
		assertEquals(2, luckyNumTypes.size());
		assertEquals(24, luckyNumTypes.get(0).luckyNum);
		assertEquals(22, luckyNumTypes.get(1).luckyNum);
		assertTrue(vcard.getExtendedTypes("X-LUCKY-NUM").isEmpty());

		//read a type without a type class
		List<RawType> genderTypes = vcard.getExtendedTypes("X-GENDER");
		assertEquals(1, genderTypes.size());
		assertEquals("ma\\,le", genderTypes.get(0).getValue()); //raw type values are not unescaped

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void readExtendedType_override_standard_type_classes() throws Throwable {
		//@formatter:off
		String str =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"FN:John Doe\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		VCardReader reader = new VCardReader(str);
		reader.registerScribe(new MyFormattedNameScribe());
		VCard vcard = reader.readNext();
		assertEquals(1, vcard.getAllTypes().size());

		//read a type that has a type class
		MyFormattedNameType fn = vcard.getType(MyFormattedNameType.class);
		assertEquals("JOHN DOE", fn.value);

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	/**
	 * Tests to make sure it can read multiple vCards from the same stream.
	 */
	@Test
	public void readMultiple() throws Throwable {
		//@formatter:off
		String str =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"FN:John Doe\r\n" +
		"END:VCARD\r\n" +
		"BEGIN:VCARD\r\n" +
		"VERSION:3.0\r\n" +
		"FN:Jane Doe\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		VCardReader reader = new VCardReader(str);
		VCard vcard;

		vcard = reader.readNext();
		assertEquals(VCardVersion.V2_1, vcard.getVersion());
		assertEquals("John Doe", vcard.getFormattedName().getValue());
		assertWarnings(0, reader.getWarnings());

		vcard = reader.readNext();
		assertEquals(VCardVersion.V3_0, vcard.getVersion());
		assertEquals("Jane Doe", vcard.getFormattedName().getValue());
		assertWarnings(0, reader.getWarnings());

		assertNull(reader.readNext());
	}

	/**
	 * Tests types with nested vCards (i.e AGENT type) in version 2.1.
	 */
	@Test
	public void nestedVCard() throws Throwable {
		//@formatter:off
		String str =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"AGENT:\r\n" +
			"BEGIN:VCARD\r\n" +
			"VERSION:2.1\r\n" +
			"FN:Agent 007\r\n" +
			"AGENT:\r\n" +
				"BEGIN:VCARD\r\n" +
				"VERSION:2.1\r\n" +
				"FN:Agent 009\r\n" +
				"END:VCARD\r\n" +
			"END:VCARD\r\n" +
		"FN:John Doe\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		VCardReader reader = new VCardReader(str);

		VCard vcard = reader.readNext();
		assertEquals("John Doe", vcard.getFormattedName().getValue());
		VCard agent1 = vcard.getAgent().getVCard();
		assertEquals("Agent 007", agent1.getFormattedName().getValue());
		VCard agent2 = agent1.getAgent().getVCard();
		assertEquals("Agent 009", agent2.getFormattedName().getValue());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void nestedVCard_missing_vcard() throws Throwable {
		//@formatter:off
		String str =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"AGENT:\r\n" +
		"FN:John Doe\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		VCardReader reader = new VCardReader(str);

		VCard vcard = reader.readNext();
		assertEquals("John Doe", vcard.getFormattedName().getValue());
		assertNull(vcard.getAgent().getVCard());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	/**
	 * Tests types with embedded vCards (i.e. AGENT type) in version 3.0.
	 */
	@Test
	public void embeddedVCard() throws Throwable {
		//@formatter:off
		String str =
		"BEGIN:VCARD\r\n" +
		"VERSION:3.0\r\n" +
		"AGENT:" +
			"BEGIN:VCARD\\n" +
			"VERSION:3.0\\n" +
			"FN:Agent 007\\n" +
			"AGENT:" +
				"BEGIN:VCARD\\\\n" +
				"VERSION:3.0\\\\n" +
				"FN:Agent 009\\\\n" +
				"END:VCARD\\\\n" +
			"END:VCARD\r\n" +
		"FN:John Doe\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		VCardReader reader = new VCardReader(str);
		VCard vcard = reader.readNext();

		assertEquals("John Doe", vcard.getFormattedName().getValue());
		VCard agent1 = vcard.getAgent().getVCard();
		assertEquals("Agent 007", agent1.getFormattedName().getValue());
		VCard agent2 = agent1.getAgent().getVCard();
		assertEquals("Agent 009", agent2.getFormattedName().getValue());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	/**
	 * LABEL types should be assigned to an ADR and stored in the
	 * "AddressType.getLabel()" field. LABELs that could not be assigned to an
	 * ADR should go in "VCard.getOrphanedLabels()".
	 */
	@Test
	public void readLabel() throws Throwable {
		//@formatter:off
		String str =
		"BEGIN:VCARD\r\n" +
		"VERSION:3.0\r\n" +
		"ADR;TYPE=home:;;123 Main St.;Austin;TX;91827;USA\r\n" +
		"LABEL;TYPE=home:123 Main St.\\nAustin\\, TX 91827\\nUSA\r\n" +
		"ADR;TYPE=work,parcel:;;200 Broadway;New York;NY;12345;USA\r\n" +
		"LABEL;TYPE=work:200 Broadway\\nNew York\\, NY 12345\\nUSA\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		VCardReader reader = new VCardReader(str);
		VCard vcard = reader.readNext();

		assertEquals(2, vcard.getAddresses().size());

		AddressType adr = vcard.getAddresses().get(0);
		assertSetEquals(adr.getTypes(), AddressTypeParameter.HOME);
		assertEquals("123 Main St." + NEWLINE + "Austin, TX 91827" + NEWLINE + "USA", adr.getLabel());

		adr = vcard.getAddresses().get(1);
		assertSetEquals(adr.getTypes(), AddressTypeParameter.WORK, AddressTypeParameter.PARCEL);
		assertNull(adr.getLabel());

		assertEquals(1, vcard.getOrphanedLabels().size());
		LabelType label = vcard.getOrphanedLabels().get(0);
		assertEquals("200 Broadway" + NEWLINE + "New York, NY 12345" + NEWLINE + "USA", label.getValue());
		assertSetEquals(label.getTypes(), AddressTypeParameter.WORK);

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	/**
	 * If the type's unmarshal method throws a {@link SkipMeException}, then a
	 * warning should be added to the warnings list and the type object should
	 * NOT be added to the {@link VCard} object.
	 */
	@Test
	public void skipMeException() throws Throwable {
		//@formatter:off
		String str =
		"BEGIN:VCARD\r\n" +
		"VERSION:3.0\r\n" +
		"X-LUCKY-NUM:24\r\n" +
		"X-LUCKY-NUM:13\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		VCardReader reader = new VCardReader(str);
		reader.registerScribe(new LuckyNumScribe());
		VCard vcard = reader.readNext();

		List<LuckyNumType> luckyNumTypes = vcard.getTypes(LuckyNumType.class);
		assertEquals(1, luckyNumTypes.size());
		assertEquals(24, luckyNumTypes.get(0).luckyNum);

		assertWarnings(1, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void invalid_line() throws Throwable {
		//@formatter:off
		String str =
		"BEGIN:VCARD\r\n" +
		"VERSION:2.1\r\n" +
		"bad-line\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		VCardReader reader = new VCardReader(str);
		reader.readNext();

		assertWarnings(1, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void invalid_version() throws Throwable {
		//@formatter:off
		String str =
		"BEGIN:VCARD\r\n" +
		"VERSION:invalid\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		VCardReader reader = new VCardReader(str);
		VCard vcard = reader.readNext();
		assertEquals(VCardVersion.V2_1, vcard.getVersion()); //default to 2.1

		assertWarnings(1, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void skip_non_vcard_components() throws Throwable {
		//@formatter:off
		String str =
		"BEGIN:VCALENDAR\r\n" +
		"VERSION:2.0\r\n" +
		"PRODID:-//Company//Application//EN" +
		"END:VCALENDAR\r\n" +
		"BEGIN:VCARD\r\n" +
		"VERSION:3.0\r\n" +
		"FN:John Doe\r\n" +
		"END:VCARD\r\n";
		//@formatter:on

		VCardReader reader = new VCardReader(str);

		VCard vcard = reader.readNext();
		assertEquals(VCardVersion.V3_0, vcard.getVersion());
		assertEquals("John Doe", vcard.getFormattedName().getValue());
		assertNull(vcard.getProdId());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void evolutionVCard() throws Throwable {
		VCardReader reader = new VCardReader(getClass().getResourceAsStream("John_Doe_EVOLUTION.vcf"));
		VCard vcard = reader.readNext();

		//VERSION
		assertEquals(VCardVersion.V3_0, vcard.getVersion());

		//URL
		{
			Iterator<UrlType> it = vcard.getUrls().iterator();

			UrlType t = it.next();
			assertEquals("http://www.ibm.com", t.getValue());
			assertEquals("0abc9b8d-0845-47d0-9a91-3db5bb74620d", t.getSubTypes().first("X-COUCHDB-UUID"));

			assertFalse(it.hasNext());
		}

		//TEL
		{
			Iterator<TelephoneType> it = vcard.getTelephoneNumbers().iterator();

			TelephoneType t = it.next();
			assertEquals("905-666-1234", t.getText());
			assertSetEquals(t.getTypes(), TelephoneTypeParameter.CELL);
			assertEquals("c2fa1caa-2926-4087-8971-609cfc7354ce", t.getSubTypes().first("X-COUCHDB-UUID"));

			t = it.next();
			assertEquals("905-555-1234", t.getText());
			assertSetEquals(t.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.VOICE);
			assertEquals("fbfb2722-4fd8-4dbf-9abd-eeb24072fd8e", t.getSubTypes().first("X-COUCHDB-UUID"));

			assertFalse(it.hasNext());
		}

		//UID
		{
			UidType t = vcard.getUid();
			assertEquals("477343c8e6bf375a9bac1f96a5000837", t.getValue());
		}

		//N
		{
			StructuredNameType t = vcard.getStructuredName();
			assertEquals("Doe", t.getFamily());
			assertEquals("John", t.getGiven());
			List<String> list = t.getAdditional();
			assertEquals(Arrays.asList("Richter, James"), list);
			list = t.getPrefixes();
			assertEquals(Arrays.asList("Mr."), list);
			list = t.getSuffixes();
			assertEquals(Arrays.asList("Sr."), list);
		}

		//FN
		{
			FormattedNameType t = vcard.getFormattedName();
			assertEquals("Mr. John Richter, James Doe Sr.", t.getValue());
		}

		//NICKNAME
		{
			NicknameType t = vcard.getNickname();
			assertEquals(Arrays.asList("Johny"), t.getValues());
		}

		//ORG
		{
			OrganizationType t = vcard.getOrganization();
			assertEquals(Arrays.asList("IBM", "Accounting", "Dungeon"), t.getValues());
		}

		//TITLE
		{
			Iterator<TitleType> it = vcard.getTitles().iterator();

			TitleType t = it.next();
			assertEquals("Money Counter", t.getValue());

			assertFalse(it.hasNext());
		}

		//CATEGORIES
		{
			CategoriesType t = vcard.getCategories();
			assertEquals(Arrays.asList("VIP"), t.getValues());
		}

		//NOTE
		{
			Iterator<NoteType> it = vcard.getNotes().iterator();
			NoteType t = it.next();
			assertEquals("THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.", t.getValue());
			assertFalse(it.hasNext());
		}

		//EMAIL
		{
			Iterator<EmailType> it = vcard.getEmails().iterator();

			EmailType t = it.next();
			assertEquals("john.doe@ibm.com", t.getValue());
			assertSetEquals(t.getTypes(), EmailTypeParameter.WORK);
			assertEquals("83a75a5d-2777-45aa-bab5-76a4bd972490", t.getSubTypes().first("X-COUCHDB-UUID"));

			assertFalse(it.hasNext());
		}

		//ADR
		{
			Iterator<AddressType> it = vcard.getAddresses().iterator();

			AddressType t = it.next();
			assertEquals("ASB-123", t.getPoBox());
			assertEquals(null, t.getExtendedAddress());
			assertEquals("15 Crescent moon drive", t.getStreetAddress());
			assertEquals("Albaney", t.getLocality());
			assertEquals("New York", t.getRegion());
			assertEquals("12345", t.getPostalCode());
			//the space between "United" and "States" is lost because it was included with the folding character and ignored (see .vcf file)
			assertEquals("UnitedStates of America", t.getCountry());
			assertSetEquals(t.getTypes(), AddressTypeParameter.HOME);

			assertFalse(it.hasNext());
		}

		//BDAY
		{
			BirthdayType t = vcard.getBirthday();
			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(Calendar.YEAR, 1980);
			c.set(Calendar.MONTH, Calendar.MARCH);
			c.set(Calendar.DAY_OF_MONTH, 22);
			Date expected = c.getTime();
			assertEquals(expected, t.getDate());
		}

		//REV
		{
			RevisionType t = vcard.getRevision();
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			c.clear();
			c.set(Calendar.YEAR, 2012);
			c.set(Calendar.MONTH, Calendar.MARCH);
			c.set(Calendar.DAY_OF_MONTH, 5);
			c.set(Calendar.HOUR_OF_DAY, 13);
			c.set(Calendar.MINUTE, 32);
			c.set(Calendar.SECOND, 54);
			assertEquals(c.getTime(), t.getValue());
		}

		//extended types
		{
			assertEquals(7, countExtTypes(vcard));

			Iterator<RawType> it = vcard.getExtendedTypes("X-COUCHDB-APPLICATION-ANNOTATIONS").iterator();
			RawType t = it.next();
			assertEquals("X-COUCHDB-APPLICATION-ANNOTATIONS", t.getPropertyName());
			assertEquals("{\"Evolution\":{\"revision\":\"2012-03-05T13:32:54Z\"}}", t.getValue());
			assertFalse(it.hasNext());

			it = vcard.getExtendedTypes("X-AIM").iterator();
			t = it.next();
			assertEquals("X-AIM", t.getPropertyName());
			assertEquals("johnny5@aol.com", t.getValue());
			assertEquals("HOME", t.getSubTypes().getType());
			assertEquals("cb9e11fc-bb97-4222-9cd8-99820c1de454", t.getSubTypes().first("X-COUCHDB-UUID"));
			assertFalse(it.hasNext());

			it = vcard.getExtendedTypes("X-EVOLUTION-FILE-AS").iterator();
			t = it.next();
			assertEquals("X-EVOLUTION-FILE-AS", t.getPropertyName());
			assertEquals("Doe\\, John", t.getValue());
			assertFalse(it.hasNext());

			it = vcard.getExtendedTypes("X-EVOLUTION-SPOUSE").iterator();
			t = it.next();
			assertEquals("X-EVOLUTION-SPOUSE", t.getPropertyName());
			assertEquals("Maria", t.getValue());
			assertFalse(it.hasNext());

			it = vcard.getExtendedTypes("X-EVOLUTION-MANAGER").iterator();
			t = it.next();
			assertEquals("X-EVOLUTION-MANAGER", t.getPropertyName());
			assertEquals("Big Blue", t.getValue());
			assertFalse(it.hasNext());

			it = vcard.getExtendedTypes("X-EVOLUTION-ASSISTANT").iterator();
			t = it.next();
			assertEquals("X-EVOLUTION-ASSISTANT", t.getPropertyName());
			assertEquals("Little Red", t.getValue());
			assertFalse(it.hasNext());

			it = vcard.getExtendedTypes("X-EVOLUTION-ANNIVERSARY").iterator();
			t = it.next();
			assertEquals("X-EVOLUTION-ANNIVERSARY", t.getPropertyName());
			assertEquals("1980-03-22", t.getValue());
			assertFalse(it.hasNext());
		}

		assertValidate(vcard.validate(VCardVersion.V3_0), vcard.getEmails().get(0));
		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void gmailVCard() throws Throwable {
		VCardReader reader = new VCardReader(getClass().getResourceAsStream("John_Doe_GMAIL.vcf"));
		VCard vcard = reader.readNext();

		//VERSION
		assertEquals(VCardVersion.V3_0, vcard.getVersion());

		//FN
		{
			FormattedNameType f = vcard.getFormattedName();
			assertEquals("Mr. John Richter, James Doe Sr.", f.getValue());
		}

		//N
		{
			StructuredNameType f = vcard.getStructuredName();
			assertEquals("Doe", f.getFamily());
			assertEquals("John", f.getGiven());
			assertEquals(Arrays.asList("Richter, James"), f.getAdditional());
			assertEquals(Arrays.asList("Mr."), f.getPrefixes());
			assertEquals(Arrays.asList("Sr."), f.getSuffixes());
		}

		//EMAIL
		{
			Iterator<EmailType> it = vcard.getEmails().iterator();

			EmailType f = it.next();
			assertEquals("john.doe@ibm.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.INTERNET, EmailTypeParameter.HOME);

			assertFalse(it.hasNext());
		}

		//TEL
		{
			Iterator<TelephoneType> it = vcard.getTelephoneNumbers().iterator();

			TelephoneType f = it.next();
			assertEquals("905-555-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.CELL);

			f = it.next();
			assertEquals("905-666-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.HOME);

			assertFalse(it.hasNext());
		}

		//ADR
		{
			Iterator<AddressType> it = vcard.getAddresses().iterator();

			AddressType f = it.next();
			assertEquals(null, f.getPoBox());
			assertEquals("Crescent moon drive" + NEWLINE + "555-asd" + NEWLINE + "Nice Area, Albaney, New York12345" + NEWLINE + "United States of America", f.getExtendedAddress());
			assertEquals(null, f.getStreetAddress());
			assertEquals(null, f.getLocality());
			assertEquals(null, f.getRegion());
			assertEquals(null, f.getPostalCode());
			assertEquals(null, f.getCountry());
			assertSetEquals(f.getTypes(), AddressTypeParameter.HOME);

			assertFalse(it.hasNext());
		}

		//ORG
		{
			OrganizationType f = vcard.getOrganization();
			assertEquals(Arrays.asList("IBM"), f.getValues());
		}

		//TITLE
		{
			Iterator<TitleType> it = vcard.getTitles().iterator();

			TitleType f = it.next();
			assertEquals("Money Counter", f.getValue());

			assertFalse(it.hasNext());
		}

		//BDAY
		{
			BirthdayType f = vcard.getBirthday();
			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(Calendar.YEAR, 1980);
			c.set(Calendar.MONTH, Calendar.MARCH);
			c.set(Calendar.DAY_OF_MONTH, 22);
			assertEquals(c.getTime(), f.getDate());
		}

		//URL
		{
			Iterator<UrlType> it = vcard.getUrls().iterator();

			UrlType f = it.next();
			assertEquals("http://www.ibm.com", f.getValue());
			assertEquals("WORK", f.getType());

			assertFalse(it.hasNext());
		}

		//NOTE
		{
			Iterator<NoteType> it = vcard.getNotes().iterator();

			NoteType f = it.next();
			assertEquals("THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE." + NEWLINE + "Favotire Color: Blue", f.getValue());

			assertFalse(it.hasNext());
		}

		//extended types
		{
			assertEquals(6, countExtTypes(vcard));

			RawType f = vcard.getExtendedTypes("X-PHONETIC-FIRST-NAME").get(0);
			assertEquals("X-PHONETIC-FIRST-NAME", f.getPropertyName());
			assertEquals("Jon", f.getValue());

			f = vcard.getExtendedTypes("X-PHONETIC-LAST-NAME").get(0);
			assertEquals("X-PHONETIC-LAST-NAME", f.getPropertyName());
			assertEquals("Dow", f.getValue());

			f = vcard.getExtendedTypes("X-ABDATE").get(0);
			assertEquals("X-ABDATE", f.getPropertyName());
			assertEquals("1975-03-01", f.getValue());
			assertEquals("item1", f.getGroup());

			f = vcard.getExtendedTypes("X-ABLABEL").get(0);
			assertEquals("X-ABLabel", f.getPropertyName());
			assertEquals("_$!<Anniversary>!$_", f.getValue());
			assertEquals("item1", f.getGroup());

			f = vcard.getExtendedTypes("X-ABLABEL").get(1);
			assertEquals("X-ABLabel", f.getPropertyName());
			assertEquals("_$!<Spouse>!$_", f.getValue());
			assertEquals("item2", f.getGroup());

			f = vcard.getExtendedTypes("X-ABRELATEDNAMES").get(0);
			assertEquals("X-ABRELATEDNAMES", f.getPropertyName());
			assertEquals("Jenny", f.getValue());
			assertEquals("item2", f.getGroup());
		}

		assertValidate(vcard.validate(VCardVersion.V3_0), vcard.getEmails().get(0));
		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	/**
	 * This vCard was generated when selecting a list of contacts and exporting
	 * them as a vCard.
	 */
	@Test
	public void gmailList() throws Throwable {
		VCardReader reader = new VCardReader(getClass().getResourceAsStream("gmail-list.vcf"));
		VCard vcard = reader.readNext();

		//VERSION
		assertEquals(VCardVersion.V3_0, vcard.getVersion());

		//FN
		{
			FormattedNameType f = vcard.getFormattedName();
			assertEquals("Arnold Smith", f.getValue());
		}

		//N
		{
			StructuredNameType f = vcard.getStructuredName();
			assertEquals("Smith", f.getFamily());
			assertEquals("Arnold", f.getGiven());
			assertTrue(f.getAdditional().isEmpty());
			assertTrue(f.getPrefixes().isEmpty());
			assertTrue(f.getSuffixes().isEmpty());
		}

		//EMAIL
		{
			Iterator<EmailType> it = vcard.getEmails().iterator();

			EmailType f = it.next();
			assertEquals("asmithk@gmail.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.INTERNET);

			assertFalse(it.hasNext());
		}

		assertValidate(vcard.validate(VCardVersion.V3_0));
		assertWarnings(0, reader.getWarnings());
		vcard = reader.readNext();

		//VERSION
		assertEquals(VCardVersion.V3_0, vcard.getVersion());

		//FN
		{
			FormattedNameType f = vcard.getFormattedName();
			assertEquals("Chris Beatle", f.getValue());
		}

		//N
		{
			StructuredNameType f = vcard.getStructuredName();
			assertEquals("Beatle", f.getFamily());
			assertEquals("Chris", f.getGiven());
			assertTrue(f.getAdditional().isEmpty());
			assertTrue(f.getPrefixes().isEmpty());
			assertTrue(f.getSuffixes().isEmpty());
		}

		//EMAIL
		{
			Iterator<EmailType> it = vcard.getEmails().iterator();

			EmailType f = it.next();
			assertEquals("chrisy55d@yahoo.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.INTERNET);

			assertFalse(it.hasNext());
		}

		assertValidate(vcard.validate(VCardVersion.V3_0));
		assertWarnings(0, reader.getWarnings());
		vcard = reader.readNext();

		//VERSION
		assertEquals(VCardVersion.V3_0, vcard.getVersion());

		//FN
		{
			FormattedNameType f = vcard.getFormattedName();
			assertEquals("Doug White", f.getValue());
		}

		//N
		{
			StructuredNameType f = vcard.getStructuredName();
			assertEquals("White", f.getFamily());
			assertEquals("Doug", f.getGiven());
			assertTrue(f.getAdditional().isEmpty());
			assertTrue(f.getPrefixes().isEmpty());
			assertTrue(f.getSuffixes().isEmpty());
		}

		//EMAIL
		{
			Iterator<EmailType> it = vcard.getEmails().iterator();

			EmailType f = it.next();
			assertEquals("dwhite@gmail.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.INTERNET);

			assertFalse(it.hasNext());
		}

		assertValidate(vcard.validate(VCardVersion.V3_0));
		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void gmailSingle() throws Throwable {
		VCardReader reader = new VCardReader(getClass().getResourceAsStream("gmail-single.vcf"));
		VCard vcard = reader.readNext();

		//VERSION
		assertEquals(VCardVersion.V3_0, vcard.getVersion());

		//FN
		{
			FormattedNameType f = vcard.getFormattedName();
			assertEquals("Greg Dartmouth", f.getValue());
		}

		//N
		{
			StructuredNameType f = vcard.getStructuredName();
			assertEquals("Dartmouth", f.getFamily());
			assertEquals("Greg", f.getGiven());
			assertTrue(f.getAdditional().isEmpty());
			assertTrue(f.getPrefixes().isEmpty());
			assertTrue(f.getSuffixes().isEmpty());
		}

		//EMAIL
		{
			Iterator<EmailType> it = vcard.getEmails().iterator();

			EmailType f = it.next();
			assertEquals("gdartmouth@hotmail.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.INTERNET);

			assertFalse(it.hasNext());
		}

		//TEL
		{
			Iterator<TelephoneType> it = vcard.getTelephoneNumbers().iterator();

			TelephoneType f = it.next();
			assertEquals("555 555 1111", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.CELL);

			f = it.next();
			assertEquals("item1", f.getGroup());
			assertEquals("555 555 2222", f.getText());
			assertSetEquals(f.getTypes());

			assertFalse(it.hasNext());
		}

		//ADR
		{
			Iterator<AddressType> it = vcard.getAddresses().iterator();

			AddressType f = it.next();
			assertEquals(null, f.getPoBox());
			assertEquals(null, f.getExtendedAddress());
			assertEquals("123 Home St" + NEWLINE + "Home City, HM 12345", f.getStreetAddress());
			assertEquals(null, f.getLocality());
			assertEquals(null, f.getRegion());
			assertEquals(null, f.getPostalCode());
			assertEquals(null, f.getCountry());
			assertSetEquals(f.getTypes(), AddressTypeParameter.HOME);

			f = it.next();
			assertEquals("item2", f.getGroup());
			assertEquals(null, f.getPoBox());
			assertEquals(null, f.getExtendedAddress());
			assertEquals("321 Custom St", f.getStreetAddress());
			assertEquals("Custom City", f.getLocality());
			assertEquals("TX", f.getRegion());
			assertEquals("98765", f.getPostalCode());
			assertEquals("USA", f.getCountry());
			assertSetEquals(f.getTypes());

			assertFalse(it.hasNext());
		}

		//ORG
		{
			OrganizationType f = vcard.getOrganization();
			assertEquals(Arrays.asList("TheCompany"), f.getValues());
		}

		//TITLE
		{
			Iterator<TitleType> it = vcard.getTitles().iterator();

			TitleType f = it.next();
			assertEquals("TheJobTitle", f.getValue());

			assertFalse(it.hasNext());
		}

		//BDAY
		{
			BirthdayType f = vcard.getBirthday();
			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(Calendar.YEAR, 1960);
			c.set(Calendar.MONTH, Calendar.SEPTEMBER);
			c.set(Calendar.DAY_OF_MONTH, 10);
			assertEquals(c.getTime(), f.getDate());
		}

		//URL
		{
			Iterator<UrlType> it = vcard.getUrls().iterator();

			UrlType f = it.next();
			assertEquals("http://TheProfile.com", f.getValue());
			assertNull(f.getType());

			assertFalse(it.hasNext());
		}

		//NOTE
		{
			Iterator<NoteType> it = vcard.getNotes().iterator();

			NoteType f = it.next();
			assertEquals("This is GMail's note field." + NEWLINE + "It should be added as a NOTE type." + NEWLINE + "ACustomField: CustomField", f.getValue());

			assertFalse(it.hasNext());
		}

		//extended types
		{
			assertEquals(12, countExtTypes(vcard));

			RawType f = vcard.getExtendedTypes("X-PHONETIC-FIRST-NAME").get(0);
			assertEquals("X-PHONETIC-FIRST-NAME", f.getPropertyName());
			assertEquals("Grregg", f.getValue());

			f = vcard.getExtendedTypes("X-PHONETIC-LAST-NAME").get(0);
			assertEquals("X-PHONETIC-LAST-NAME", f.getPropertyName());
			assertEquals("Dart-mowth", f.getValue());

			f = vcard.getExtendedTypes("X-ICQ").get(0);
			assertEquals("X-ICQ", f.getPropertyName());
			assertEquals("123456789", f.getValue());

			Iterator<RawType> abLabelIt = vcard.getExtendedTypes("X-ABLABEL").iterator();
			{
				f = abLabelIt.next();
				assertEquals("item1", f.getGroup());
				assertEquals("GRAND_CENTRAL", f.getValue());

				f = abLabelIt.next();
				assertEquals("item2", f.getGroup());
				assertEquals("CustomAdrType", f.getValue());

				f = abLabelIt.next();
				assertEquals("item3", f.getGroup());
				assertEquals("PROFILE", f.getValue());

				f = abLabelIt.next();
				assertEquals("item4", f.getGroup());
				assertEquals("_$!<Anniversary>!$_", f.getValue());

				f = abLabelIt.next();
				assertEquals("item5", f.getGroup());
				assertEquals("_$!<Spouse>!$_", f.getValue());

				f = abLabelIt.next();
				assertEquals("item6", f.getGroup());
				assertEquals("CustomRelationship", f.getValue());

				assertFalse(abLabelIt.hasNext());
			}

			f = vcard.getExtendedTypes("X-ABDATE").get(0);
			assertEquals("item4", f.getGroup());
			assertEquals("X-ABDATE", f.getPropertyName());
			assertEquals("1970-06-02", f.getValue());

			f = vcard.getExtendedTypes("X-ABRELATEDNAMES").get(0);
			assertEquals("item5", f.getGroup());
			assertEquals("X-ABRELATEDNAMES", f.getPropertyName());
			assertEquals("MySpouse", f.getValue());

			f = vcard.getExtendedTypes("X-ABRELATEDNAMES").get(1);
			assertEquals("item6", f.getGroup());
			assertEquals("X-ABRELATEDNAMES", f.getPropertyName());
			assertEquals("MyCustom", f.getValue());
		}

		assertValidate(vcard.validate(VCardVersion.V3_0));
		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void iPhoneVCard() throws Throwable {
		VCardReader reader = new VCardReader(getClass().getResourceAsStream("John_Doe_IPHONE.vcf"));
		VCard vcard = reader.readNext();

		//VERSION
		assertEquals(VCardVersion.V3_0, vcard.getVersion());

		//PRODID
		{
			ProdIdType f = vcard.getProdId();
			assertEquals("-//Apple Inc.//iOS 5.0.1//EN", f.getValue());
		}

		//N
		{
			StructuredNameType f = vcard.getStructuredName();
			assertEquals("Doe", f.getFamily());
			assertEquals("John", f.getGiven());
			assertEquals(Arrays.asList("Richter", "James"), f.getAdditional());
			assertEquals(Arrays.asList("Mr."), f.getPrefixes());
			assertEquals(Arrays.asList("Sr."), f.getSuffixes());
		}

		//FN
		{
			FormattedNameType f = vcard.getFormattedName();
			assertEquals("Mr. John Richter James Doe Sr.", f.getValue());
		}

		//NICKNAME
		{
			NicknameType f = vcard.getNickname();
			assertEquals(Arrays.asList("Johny"), f.getValues());
		}

		//ORG
		{
			OrganizationType f = vcard.getOrganization();
			assertEquals(Arrays.asList("IBM", "Accounting"), f.getValues());
		}

		//TITLE
		{
			Iterator<TitleType> it = vcard.getTitles().iterator();

			TitleType f = it.next();
			assertEquals("Money Counter", f.getValue());

			assertFalse(it.hasNext());
		}

		//EMAIL
		{
			Iterator<EmailType> it = vcard.getEmails().iterator();

			EmailType f = it.next();
			assertEquals("item1", f.getGroup());
			assertEquals("john.doe@ibm.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.INTERNET, EmailTypeParameter.PREF);

			assertFalse(it.hasNext());
		}

		//TEL
		{
			Iterator<TelephoneType> it = vcard.getTelephoneNumbers().iterator();

			TelephoneType f = it.next();
			assertEquals("905-555-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.CELL, TelephoneTypeParameter.VOICE, TelephoneTypeParameter.PREF);

			f = it.next();
			assertEquals("905-666-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.HOME, TelephoneTypeParameter.VOICE);

			f = it.next();
			assertEquals("905-777-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.VOICE);

			f = it.next();
			assertEquals("905-888-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.HOME, TelephoneTypeParameter.FAX);

			f = it.next();
			assertEquals("905-999-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.FAX);

			f = it.next();
			assertEquals("905-111-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.PAGER);

			f = it.next();
			assertEquals("905-222-1234", f.getText());
			assertEquals("item2", f.getGroup());
			assertSetEquals(f.getTypes());

			assertFalse(it.hasNext());
		}

		//ADR
		{
			Iterator<AddressType> it = vcard.getAddresses().iterator();

			AddressType f = it.next();
			assertEquals("item3", f.getGroup());
			assertEquals(null, f.getPoBox());
			assertEquals(null, f.getExtendedAddress());
			assertEquals("Silicon Alley 5", f.getStreetAddress());
			assertEquals("New York", f.getLocality());
			assertEquals("New York", f.getRegion());
			assertEquals("12345", f.getPostalCode());
			assertEquals("United States of America", f.getCountry());
			assertSetEquals(f.getTypes(), AddressTypeParameter.HOME, AddressTypeParameter.PREF);

			f = it.next();
			assertEquals("item4", f.getGroup());
			assertEquals(null, f.getPoBox());
			assertEquals(null, f.getExtendedAddress());
			assertEquals("Street4" + NEWLINE + "Building 6" + NEWLINE + "Floor 8", f.getStreetAddress());
			assertEquals("New York", f.getLocality());
			assertEquals(null, f.getRegion());
			assertEquals("12345", f.getPostalCode());
			assertEquals("USA", f.getCountry());

			assertSetEquals(f.getTypes(), AddressTypeParameter.WORK);

			assertFalse(it.hasNext());
		}

		//URL
		{
			Iterator<UrlType> it = vcard.getUrls().iterator();

			UrlType f = it.next();
			assertEquals("item5", f.getGroup());
			assertEquals("http://www.ibm.com", f.getValue());
			assertEquals("pref", f.getType());

			assertFalse(it.hasNext());
		}

		//BDAY
		{
			BirthdayType f = vcard.getBirthday();
			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(Calendar.YEAR, 2012);
			c.set(Calendar.MONTH, Calendar.JUNE);
			c.set(Calendar.DAY_OF_MONTH, 6);
			assertEquals(c.getTime(), f.getDate());
		}

		//PHOTO
		{
			Iterator<PhotoType> it = vcard.getPhotos().iterator();

			PhotoType f = it.next();
			assertEquals(ImageTypeParameter.JPEG, f.getContentType());
			assertEquals(32531, f.getData().length);
		}

		//extended types
		{
			assertEquals(4, countExtTypes(vcard));

			RawType f = vcard.getExtendedTypes("X-ABLABEL").get(0);
			assertEquals("item2", f.getGroup());
			assertEquals("X-ABLabel", f.getPropertyName());
			assertEquals("_$!<AssistantPhone>!$_", f.getValue());

			f = vcard.getExtendedTypes("X-ABADR").get(0);
			assertEquals("item3", f.getGroup());
			assertEquals("X-ABADR", f.getPropertyName());
			assertEquals("Silicon Alley", f.getValue());

			f = vcard.getExtendedTypes("X-ABADR").get(1);
			assertEquals("item4", f.getGroup());
			assertEquals("X-ABADR", f.getPropertyName());
			assertEquals("Street 4, Building 6,\\n Floor 8\\nNew York\\nUSA", f.getValue());

			f = vcard.getExtendedTypes("X-ABLABEL").get(1);
			assertEquals("item5", f.getGroup());
			assertEquals("X-ABLabel", f.getPropertyName());
			assertEquals("_$!<HomePage>!$_", f.getValue());
		}

		assertValidate(vcard.validate(VCardVersion.V3_0));
		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void lotusNotesVCard() throws Throwable {
		VCardReader reader = new VCardReader(getClass().getResourceAsStream("John_Doe_LOTUS_NOTES.vcf"));
		VCard vcard = reader.readNext();

		//VERSION
		assertEquals(VCardVersion.V3_0, vcard.getVersion());

		//PRODID
		{
			ProdIdType f = vcard.getProdId();
			assertEquals("-//Apple Inc.//Address Book 6.1//EN", f.getValue());
		}

		//N
		{
			StructuredNameType f = vcard.getStructuredName();
			assertEquals("Doe", f.getFamily());
			assertEquals("John", f.getGiven());
			assertEquals(Arrays.asList("Johny"), f.getAdditional());
			assertEquals(Arrays.asList("Mr."), f.getPrefixes());
			assertEquals(Arrays.asList("I"), f.getSuffixes());
		}

		//FN
		{
			FormattedNameType f = vcard.getFormattedName();
			assertEquals("Mr. Doe John I Johny", f.getValue());
		}

		//NICKNAME
		{
			NicknameType f = vcard.getNickname();
			assertEquals(Arrays.asList("Johny,JayJay"), f.getValues());
		}

		//ORG
		{
			OrganizationType f = vcard.getOrganization();
			assertEquals(Arrays.asList("IBM", "SUN"), f.getValues());
		}

		//TITLE
		{
			Iterator<TitleType> it = vcard.getTitles().iterator();

			TitleType f = it.next();
			assertEquals("Generic Accountant", f.getValue());

			assertFalse(it.hasNext());
		}

		//EMAIL
		{
			Iterator<EmailType> it = vcard.getEmails().iterator();

			EmailType f = it.next();
			assertEquals("john.doe@ibm.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.INTERNET, EmailTypeParameter.WORK, EmailTypeParameter.PREF);

			f = it.next();
			assertEquals("billy_bob@gmail.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.INTERNET, EmailTypeParameter.WORK);

			assertFalse(it.hasNext());
		}

		//TEL
		{
			Iterator<TelephoneType> it = vcard.getTelephoneNumbers().iterator();
			TelephoneType f = it.next();
			assertEquals("+1 (212) 204-34456", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.CELL, TelephoneTypeParameter.VOICE, TelephoneTypeParameter.PREF);

			f = it.next();
			assertEquals("00-1-212-555-7777", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.FAX);

			assertFalse(it.hasNext());
		}

		//ADR
		{
			Iterator<AddressType> it = vcard.getAddresses().iterator();

			AddressType f = it.next();
			assertEquals("item1", f.getGroup());
			assertEquals(null, f.getPoBox());
			assertEquals(null, f.getExtendedAddress());
			assertEquals("25334" + NEWLINE + "South cresent drive, Building 5, 3rd floo r", f.getStreetAddress());
			assertEquals("New York", f.getLocality());
			assertEquals("New York", f.getRegion());
			assertEquals("NYC887", f.getPostalCode());
			assertEquals("U.S.A.", f.getCountry());
			assertNull(f.getLabel());
			assertSetEquals(f.getTypes(), AddressTypeParameter.HOME, AddressTypeParameter.PREF);

			assertFalse(it.hasNext());
		}

		//NOTE
		{
			Iterator<NoteType> it = vcard.getNotes().iterator();

			NoteType f = it.next();
			assertEquals("THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"" + NEWLINE + "AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO , THE" + NEWLINE + "IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR P URPOSE" + NEWLINE + "ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTOR S BE" + NEWLINE + "LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR" + NEWLINE + "CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF" + NEWLINE + " SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS " + NEWLINE + "INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN" + NEWLINE + " CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)" + NEWLINE + "A RISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE" + NEWLINE + " POSSIBILITY OF SUCH DAMAGE.", f.getValue());

			assertFalse(it.hasNext());
		}

		//URL
		{
			Iterator<UrlType> it = vcard.getUrls().iterator();

			UrlType f = it.next();
			assertEquals("item2", f.getGroup());
			assertEquals("http://www.sun.com", f.getValue());
			assertEquals("pref", f.getType());

			assertFalse(it.hasNext());
		}

		//BDAY
		{
			BirthdayType f = vcard.getBirthday();
			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(Calendar.YEAR, 1980);
			c.set(Calendar.MONTH, Calendar.MAY);
			c.set(Calendar.DAY_OF_MONTH, 21);
			assertEquals(c.getTime(), f.getDate());
		}

		//PHOTO
		{
			Iterator<PhotoType> it = vcard.getPhotos().iterator();

			PhotoType f = it.next();
			assertEquals(ImageTypeParameter.JPEG, f.getContentType());
			assertEquals(7957, f.getData().length);

			assertFalse(it.hasNext());
		}

		//UID
		{
			UidType f = vcard.getUid();
			assertEquals("0e7602cc-443e-4b82-b4b1-90f62f99a199", f.getValue());
		}

		//GEO
		{
			GeoType f = vcard.getGeo();
			assertEquals(-2.6, f.getLatitude(), .01);
			assertEquals(3.4, f.getLongitude(), .01);
		}

		//CLASS
		{
			ClassificationType f = vcard.getClassification();
			assertEquals("Public", f.getValue());
		}

		//PROFILE
		{
			ProfileType f = vcard.getProfile();
			assertEquals("VCard", f.getValue());
		}

		//TZ
		{
			TimezoneType f = vcard.getTimezone();
			assertIntEquals(1, f.getHourOffset());
			assertIntEquals(0, f.getMinuteOffset());
		}

		//LABEL
		{
			Iterator<LabelType> it = vcard.getOrphanedLabels().iterator();

			LabelType f = it.next();
			assertEquals("John Doe" + NEWLINE + "New York, NewYork," + NEWLINE + "South Crecent Drive," + NEWLINE + "Building 5, floor 3," + NEWLINE + "USA", f.getValue());
			assertSetEquals(f.getTypes(), AddressTypeParameter.HOME, AddressTypeParameter.PARCEL, AddressTypeParameter.PREF);

			assertFalse(it.hasNext());
		}

		//SORT-STRING
		{
			SortStringType f = vcard.getSortString();
			assertEquals("JOHN", f.getValue());
		}

		//ROLE
		{
			Iterator<RoleType> it = vcard.getRoles().iterator();

			RoleType f = it.next();
			assertEquals("Counting Money", f.getValue());

			assertFalse(it.hasNext());
		}

		//SOURCE
		{
			Iterator<SourceType> it = vcard.getSources().iterator();

			SourceType f = it.next();
			assertEquals("Whatever", f.getValue());

			assertFalse(it.hasNext());
		}

		//MAILER
		{
			MailerType f = vcard.getMailer();
			assertEquals("Mozilla Thunderbird", f.getValue());
		}

		//NAME
		{
			SourceDisplayTextType f = vcard.getSourceDisplayText();
			assertEquals("VCard for John Doe", f.getValue());
		}

		//extended types
		{
			assertEquals(4, countExtTypes(vcard));

			RawType f = vcard.getExtendedTypes("X-ABLABEL").get(0);
			assertEquals("item2", f.getGroup());
			assertEquals("X-ABLabel", f.getPropertyName());
			assertEquals("_$!<HomePage>!$_", f.getValue());

			f = vcard.getExtendedTypes("X-ABUID").get(0);
			assertEquals("X-ABUID", f.getPropertyName());
			assertEquals("0E7602CC-443E-4B82-B4B1-90F62F99A199:ABPerson", f.getValue());

			f = vcard.getExtendedTypes("X-GENERATOR").get(0);
			assertEquals("X-GENERATOR", f.getPropertyName());
			assertEquals("Cardme Generator", f.getValue());

			f = vcard.getExtendedTypes("X-LONG-STRING").get(0);
			assertEquals("X-LONG-STRING", f.getPropertyName());
			assertEquals("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890", f.getValue());
		}

		assertValidate(vcard.validate(VCardVersion.V3_0), vcard.getEmails().get(0), vcard.getEmails().get(1));
		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void msOutlookVCard() throws Throwable {
		VCardReader reader = new VCardReader(getClass().getResourceAsStream("John_Doe_MS_OUTLOOK.vcf"));
		VCard vcard = reader.readNext();

		//VERSION
		assertEquals(VCardVersion.V2_1, vcard.getVersion());

		//N
		{
			StructuredNameType f = vcard.getStructuredName();
			assertEquals("en-us", f.getSubTypes().getLanguage());
			assertEquals("Doe", f.getFamily());
			assertEquals("John", f.getGiven());
			assertEquals(Arrays.asList("Richter", "James"), f.getAdditional());
			assertEquals(Arrays.asList("Mr."), f.getPrefixes());
			assertEquals(Arrays.asList("Sr."), f.getSuffixes());
		}

		//FN
		{
			FormattedNameType f = vcard.getFormattedName();
			assertEquals("Mr. John Richter James Doe Sr.", f.getValue());
		}

		//NICKNAME
		{
			NicknameType f = vcard.getNickname();
			assertEquals(Arrays.asList("Johny"), f.getValues());
		}

		//ORG
		{
			OrganizationType f = vcard.getOrganization();
			assertEquals(Arrays.asList("IBM", "Accounting"), f.getValues());
		}

		//TITLE
		{
			Iterator<TitleType> it = vcard.getTitles().iterator();

			TitleType f = it.next();
			assertEquals("Money Counter", f.getValue());

			assertFalse(it.hasNext());
		}

		//NOTE
		{
			Iterator<NoteType> it = vcard.getNotes().iterator();

			NoteType f = it.next();
			assertEquals("THIS SOFTWARE IS PROVIDED BY GEORGE EL-HADDAD ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GEORGE EL-HADDAD OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.", f.getValue());

			assertFalse(it.hasNext());
		}

		//TEL
		{
			Iterator<TelephoneType> it = vcard.getTelephoneNumbers().iterator();
			TelephoneType f = it.next();

			assertEquals("(905) 555-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.VOICE);

			f = it.next();
			assertEquals("(905) 666-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.HOME, TelephoneTypeParameter.VOICE);

			assertFalse(it.hasNext());
		}

		//ADR
		{
			Iterator<AddressType> it = vcard.getAddresses().iterator();

			AddressType f = it.next();
			assertEquals(null, f.getPoBox());
			assertEquals(null, f.getExtendedAddress());
			assertEquals("Cresent moon drive", f.getStreetAddress());
			assertEquals("Albaney", f.getLocality());
			assertEquals("New York", f.getRegion());
			assertEquals("12345", f.getPostalCode());
			assertEquals("United States of America", f.getCountry());
			assertEquals("Cresent moon drive\r\nAlbaney, New York  12345", f.getLabel());
			assertSetEquals(f.getTypes(), AddressTypeParameter.WORK, AddressTypeParameter.PREF);

			f = it.next();
			assertEquals(null, f.getPoBox());
			assertEquals(null, f.getExtendedAddress());
			assertEquals("Silicon Alley 5", f.getStreetAddress());
			assertEquals("New York", f.getLocality());
			assertEquals("New York", f.getRegion());
			assertEquals("12345", f.getPostalCode());
			assertEquals("United States of America", f.getCountry());
			assertEquals("Silicon Alley 5,\r\nNew York, New York  12345", f.getLabel());
			assertSetEquals(f.getTypes(), AddressTypeParameter.HOME);

			assertFalse(it.hasNext());
		}

		//LABEL
		{
			assertTrue(vcard.getOrphanedLabels().isEmpty());
		}

		//URL
		{
			Iterator<UrlType> it = vcard.getUrls().iterator();

			UrlType f = it.next();
			assertEquals("http://www.ibm.com", f.getValue());
			assertEquals("WORK", f.getType());

			assertFalse(it.hasNext());
		}

		//ROLE
		{
			Iterator<RoleType> it = vcard.getRoles().iterator();

			RoleType f = it.next();
			assertEquals("Counting Money", f.getValue());

			assertFalse(it.hasNext());
		}

		//BDAY
		{
			BirthdayType f = vcard.getBirthday();
			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(Calendar.YEAR, 1980);
			c.set(Calendar.MONTH, Calendar.MARCH);
			c.set(Calendar.DAY_OF_MONTH, 22);
			assertEquals(c.getTime(), f.getDate());
		}

		//EMAIL
		{
			Iterator<EmailType> it = vcard.getEmails().iterator();

			EmailType f = it.next();
			assertEquals("john.doe@ibm.cm", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.PREF, EmailTypeParameter.INTERNET);

			assertFalse(it.hasNext());
		}

		//PHOTO
		{
			Iterator<PhotoType> it = vcard.getPhotos().iterator();

			PhotoType f = it.next();
			assertEquals(ImageTypeParameter.JPEG, f.getContentType());
			assertEquals(860, f.getData().length);

			assertFalse(it.hasNext());
		}

		//REV
		{
			RevisionType f = vcard.getRevision();
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			c.clear();
			c.set(Calendar.YEAR, 2012);
			c.set(Calendar.MONTH, Calendar.MARCH);
			c.set(Calendar.DAY_OF_MONTH, 5);
			c.set(Calendar.HOUR_OF_DAY, 13);
			c.set(Calendar.MINUTE, 19);
			c.set(Calendar.SECOND, 33);
			assertEquals(c.getTime(), f.getValue());
		}

		//extended types
		{
			assertEquals(6, countExtTypes(vcard));

			RawType f = vcard.getExtendedTypes("X-MS-OL-DEFAULT-POSTAL-ADDRESS").get(0);
			assertEquals("X-MS-OL-DEFAULT-POSTAL-ADDRESS", f.getPropertyName());
			assertEquals("2", f.getValue());

			f = vcard.getExtendedTypes("X-MS-ANNIVERSARY").get(0);
			assertEquals("X-MS-ANNIVERSARY", f.getPropertyName());
			assertEquals("20110113", f.getValue());

			f = vcard.getExtendedTypes("X-MS-IMADDRESS").get(0);
			assertEquals("X-MS-IMADDRESS", f.getPropertyName());
			assertEquals("johny5@aol.com", f.getValue());

			f = vcard.getExtendedTypes("X-MS-OL-DESIGN").get(0);
			assertEquals("X-MS-OL-DESIGN", f.getPropertyName());
			assertEquals("<card xmlns=\"http://schemas.microsoft.com/office/outlook/12/electronicbusinesscards\" ver=\"1.0\" layout=\"left\" bgcolor=\"ffffff\"><img xmlns=\"\" align=\"tleft\" area=\"32\" use=\"photo\"/><fld xmlns=\"\" prop=\"name\" align=\"left\" dir=\"ltr\" style=\"b\" color=\"000000\" size=\"10\"/><fld xmlns=\"\" prop=\"org\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"/><fld xmlns=\"\" prop=\"title\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"/><fld xmlns=\"\" prop=\"dept\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"/><fld xmlns=\"\" prop=\"telwork\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"><label align=\"right\" color=\"626262\">Work</label></fld><fld xmlns=\"\" prop=\"telhome\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"><label align=\"right\" color=\"626262\">Home</label></fld><fld xmlns=\"\" prop=\"email\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"/><fld xmlns=\"\" prop=\"addrwork\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"/><fld xmlns=\"\" prop=\"addrhome\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"/><fld xmlns=\"\" prop=\"webwork\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"/><fld xmlns=\"\" prop=\"blank\" size=\"8\"/><fld xmlns=\"\" prop=\"blank\" size=\"8\"/><fld xmlns=\"\" prop=\"blank\" size=\"8\"/><fld xmlns=\"\" prop=\"blank\" size=\"8\"/><fld xmlns=\"\" prop=\"blank\" size=\"8\"/><fld xmlns=\"\" prop=\"blank\" size=\"8\"/></card>", f.getValue());
			assertEquals("utf-8", f.getSubTypes().getCharset());

			f = vcard.getExtendedTypes("X-MS-MANAGER").get(0);
			assertEquals("X-MS-MANAGER", f.getPropertyName());
			assertEquals("Big Blue", f.getValue());

			f = vcard.getExtendedTypes("X-MS-ASSISTANT").get(0);
			assertEquals("X-MS-ASSISTANT", f.getPropertyName());
			assertEquals("Jenny", f.getValue());
		}

		assertValidate(vcard.validate(VCardVersion.V2_1), vcard.getNickname());
		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void outlook2007VCard() throws Throwable {
		VCardReader reader = new VCardReader(getClass().getResourceAsStream("outlook-2007.vcf"));
		VCard vcard = reader.readNext();

		//VERSION
		assertEquals(VCardVersion.V2_1, vcard.getVersion());

		//N
		{
			StructuredNameType f = vcard.getStructuredName();
			assertEquals("en-us", f.getSubTypes().getLanguage());
			assertEquals("Angstadt", f.getFamily());
			assertEquals("Michael", f.getGiven());
			assertTrue(f.getAdditional().isEmpty());
			assertEquals(Arrays.asList("Mr."), f.getPrefixes());
			assertEquals(Arrays.asList("Jr."), f.getSuffixes());
		}

		//FN
		{
			FormattedNameType f = vcard.getFormattedName();
			assertEquals("Mr. Michael Angstadt Jr.", f.getValue());
		}

		//NICKNAME
		{
			NicknameType f = vcard.getNickname();
			assertEquals(Arrays.asList("Mike"), f.getValues());
		}

		//ORG
		{
			OrganizationType f = vcard.getOrganization();
			assertEquals(Arrays.asList("TheCompany", "TheDepartment"), f.getValues());
		}

		//TITLE
		{
			Iterator<TitleType> it = vcard.getTitles().iterator();

			TitleType f = it.next();
			assertEquals("TheJobTitle", f.getValue());

			assertFalse(it.hasNext());
		}

		//NOTE
		{
			Iterator<NoteType> it = vcard.getNotes().iterator();

			NoteType f = it.next();
			assertEquals("This is the NOTE field	\r\nI assume it encodes this text inside a NOTE vCard type.\r\nBut I'm not sure because there's text formatting going on here.\r\nIt does not preserve the formatting", f.getValue());
			assertEquals("us-ascii", f.getSubTypes().getCharset());

			assertFalse(it.hasNext());
		}

		//TEL
		{
			Iterator<TelephoneType> it = vcard.getTelephoneNumbers().iterator();
			TelephoneType f = it.next();

			assertEquals("(111) 555-1111", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.VOICE);

			f = it.next();
			assertEquals("(111) 555-2222", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.HOME, TelephoneTypeParameter.VOICE);

			f = it.next();
			assertEquals("(111) 555-4444", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.CELL, TelephoneTypeParameter.VOICE);

			f = it.next();
			assertEquals("(111) 555-3333", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.FAX, TelephoneTypeParameter.WORK);

			assertFalse(it.hasNext());
		}

		//ADR
		{
			Iterator<AddressType> it = vcard.getAddresses().iterator();

			AddressType f = it.next();
			assertEquals(null, f.getPoBox());
			assertEquals("TheOffice", f.getExtendedAddress());
			assertEquals("222 Broadway", f.getStreetAddress());
			assertEquals("New York", f.getLocality());
			assertEquals("NY", f.getRegion());
			assertEquals("99999", f.getPostalCode());
			assertEquals("USA", f.getCountry());
			assertEquals("222 Broadway\r\nNew York, NY 99999\r\nUSA", f.getLabel());
			assertSetEquals(f.getTypes(), AddressTypeParameter.WORK, AddressTypeParameter.PREF);

			assertFalse(it.hasNext());
		}

		//LABEL
		{
			assertTrue(vcard.getOrphanedLabels().isEmpty());
		}

		//URL
		{
			Iterator<UrlType> it = vcard.getUrls().iterator();

			UrlType f = it.next();
			assertEquals("http://mikeangstadt.name", f.getValue());
			assertEquals("HOME", f.getType());

			f = it.next();
			assertEquals("http://mikeangstadt.name", f.getValue());
			assertEquals("WORK", f.getType());

			assertFalse(it.hasNext());
		}

		//ROLE
		{
			Iterator<RoleType> it = vcard.getRoles().iterator();

			RoleType f = it.next();
			assertEquals("TheProfession", f.getValue());

			assertFalse(it.hasNext());
		}

		//BDAY
		{
			BirthdayType f = vcard.getBirthday();
			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(Calendar.YEAR, 1922);
			c.set(Calendar.MONTH, Calendar.MARCH);
			c.set(Calendar.DAY_OF_MONTH, 10);
			assertEquals(c.getTime(), f.getDate());
		}

		//KEY
		{
			Iterator<KeyType> it = vcard.getKeys().iterator();

			KeyType f = it.next();
			assertEquals(KeyTypeParameter.X509, f.getContentType());
			assertEquals(514, f.getData().length);

			assertFalse(it.hasNext());
		}

		//EMAIL
		{
			Iterator<EmailType> it = vcard.getEmails().iterator();

			EmailType f = it.next();
			assertEquals("mike.angstadt@gmail.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.PREF, EmailTypeParameter.INTERNET);

			assertFalse(it.hasNext());
		}

		//PHOTO
		{
			Iterator<PhotoType> it = vcard.getPhotos().iterator();

			PhotoType f = it.next();
			assertEquals(ImageTypeParameter.JPEG, f.getContentType());
			assertEquals(2324, f.getData().length);

			assertFalse(it.hasNext());
		}

		//FBURL
		{
			//a 4.0 property in a 2.1 vCard...
			Iterator<FbUrlType> it = vcard.getFbUrls().iterator();

			FbUrlType f = it.next();
			assertEquals("http://website.com/mycal", f.getValue());

			assertFalse(it.hasNext());
		}

		//REV
		{
			RevisionType f = vcard.getRevision();
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			c.clear();
			c.set(Calendar.YEAR, 2012);
			c.set(Calendar.MONTH, Calendar.AUGUST);
			c.set(Calendar.DAY_OF_MONTH, 1);
			c.set(Calendar.HOUR_OF_DAY, 18);
			c.set(Calendar.MINUTE, 46);
			c.set(Calendar.SECOND, 31);
			assertEquals(c.getTime(), f.getValue());
		}

		//extended types
		{
			assertEquals(8, countExtTypes(vcard));

			RawType f = vcard.getExtendedTypes("X-MS-TEL").get(0);
			assertEquals("X-MS-TEL", f.getPropertyName());
			assertEquals("(111) 555-4444", f.getValue());
			assertSetEquals(f.getSubTypes().getTypes(), "VOICE", "CALLBACK");

			f = vcard.getExtendedTypes("X-MS-OL-DEFAULT-POSTAL-ADDRESS").get(0);
			assertEquals("X-MS-OL-DEFAULT-POSTAL-ADDRESS", f.getPropertyName());
			assertEquals("2", f.getValue());

			f = vcard.getExtendedTypes("X-MS-ANNIVERSARY").get(0);
			assertEquals("X-MS-ANNIVERSARY", f.getPropertyName());
			assertEquals("20120801", f.getValue());

			f = vcard.getExtendedTypes("X-MS-IMADDRESS").get(0);
			assertEquals("X-MS-IMADDRESS", f.getPropertyName());
			assertEquals("im@aim.com", f.getValue());

			f = vcard.getExtendedTypes("X-MS-OL-DESIGN").get(0);
			assertEquals("X-MS-OL-DESIGN", f.getPropertyName());
			assertEquals("<card xmlns=\"http://schemas.microsoft.com/office/outlook/12/electronicbusinesscards\" ver=\"1.0\" layout=\"left\" bgcolor=\"ffffff\"><img xmlns=\"\" align=\"tleft\" area=\"32\" use=\"photo\"/><fld xmlns=\"\" prop=\"name\" align=\"left\" dir=\"ltr\" style=\"b\" color=\"000000\" size=\"10\"/><fld xmlns=\"\" prop=\"org\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"/><fld xmlns=\"\" prop=\"title\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"/><fld xmlns=\"\" prop=\"dept\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"/><fld xmlns=\"\" prop=\"telwork\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"><label align=\"right\" color=\"626262\">Work</label></fld><fld xmlns=\"\" prop=\"telcell\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"><label align=\"right\" color=\"626262\">Mobile</label></fld><fld xmlns=\"\" prop=\"telhome\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"><label align=\"right\" color=\"626262\">Home</label></fld><fld xmlns=\"\" prop=\"email\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"/><fld xmlns=\"\" prop=\"addrwork\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"/><fld xmlns=\"\" prop=\"webwork\" align=\"left\" dir=\"ltr\" color=\"000000\" size=\"8\"/><fld xmlns=\"\" prop=\"blank\" size=\"8\"/><fld xmlns=\"\" prop=\"blank\" size=\"8\"/><fld xmlns=\"\" prop=\"blank\" size=\"8\"/><fld xmlns=\"\" prop=\"blank\" size=\"8\"/><fld xmlns=\"\" prop=\"blank\" size=\"8\"/><fld xmlns=\"\" prop=\"blank\" size=\"8\"/></card>", f.getValue());
			assertEquals("utf-8", f.getSubTypes().getCharset());

			f = vcard.getExtendedTypes("X-MS-MANAGER").get(0);
			assertEquals("X-MS-MANAGER", f.getPropertyName());
			assertEquals("TheManagerName", f.getValue());

			f = vcard.getExtendedTypes("X-MS-ASSISTANT").get(0);
			assertEquals("X-MS-ASSISTANT", f.getPropertyName());
			assertEquals("TheAssistantName", f.getValue());

			f = vcard.getExtendedTypes("X-MS-SPOUSE").get(0);
			assertEquals("X-MS-SPOUSE", f.getPropertyName());
			assertEquals("TheSpouse", f.getValue());
		}

		assertValidate(vcard.validate(VCardVersion.V2_1), vcard.getNickname(), vcard.getFbUrls().get(0));
		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void macAddressBookVCard() throws Throwable {
		VCardReader reader = new VCardReader(getClass().getResourceAsStream("John_Doe_MAC_ADDRESS_BOOK.vcf"));
		VCard vcard = reader.readNext();

		//VERSION
		assertEquals(VCardVersion.V3_0, vcard.getVersion());

		//N
		{
			StructuredNameType f = vcard.getStructuredName();
			assertEquals("Doe", f.getFamily());
			assertEquals("John", f.getGiven());
			assertEquals(Arrays.asList("Richter,James"), f.getAdditional());
			assertEquals(Arrays.asList("Mr."), f.getPrefixes());
			assertEquals(Arrays.asList("Sr."), f.getSuffixes());
		}

		//FN
		{
			FormattedNameType f = vcard.getFormattedName();
			assertEquals("Mr. John Richter,James Doe Sr.", f.getValue());
		}

		//NICKNAME
		{
			NicknameType f = vcard.getNickname();
			assertEquals(Arrays.asList("Johny"), f.getValues());
		}

		//ORG
		{
			OrganizationType f = vcard.getOrganization();
			assertEquals(Arrays.asList("IBM", "Accounting"), f.getValues());
		}

		//TITLE
		{
			Iterator<TitleType> it = vcard.getTitles().iterator();

			TitleType f = it.next();
			assertEquals("Money Counter", f.getValue());

			assertFalse(it.hasNext());
		}

		//EMAIL
		{
			Iterator<EmailType> it = vcard.getEmails().iterator();

			EmailType f = it.next();
			assertEquals("john.doe@ibm.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.INTERNET, EmailTypeParameter.WORK, EmailTypeParameter.PREF);

			assertFalse(it.hasNext());
		}

		//TEL
		{
			Iterator<TelephoneType> it = vcard.getTelephoneNumbers().iterator();

			TelephoneType f = it.next();
			assertEquals("905-777-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.PREF);

			f = it.next();
			assertEquals("905-666-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.HOME);

			f = it.next();
			assertEquals("905-555-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.CELL);

			f = it.next();
			assertEquals("905-888-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.HOME, TelephoneTypeParameter.FAX);

			f = it.next();
			assertEquals("905-999-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.FAX);

			f = it.next();
			assertEquals("905-111-1234", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.PAGER);

			f = it.next();
			assertEquals("905-222-1234", f.getText());
			assertEquals("item1", f.getGroup());
			assertSetEquals(f.getTypes());

			assertFalse(it.hasNext());
		}

		//ADR
		{
			Iterator<AddressType> it = vcard.getAddresses().iterator();

			AddressType f = it.next();
			assertEquals("item2", f.getGroup());
			assertEquals(null, f.getPoBox());
			assertEquals(null, f.getExtendedAddress());
			assertEquals("Silicon Alley 5,", f.getStreetAddress());
			assertEquals("New York", f.getLocality());
			assertEquals("New York", f.getRegion());
			assertEquals("12345", f.getPostalCode());
			assertEquals("United States of America", f.getCountry());
			assertSetEquals(f.getTypes(), AddressTypeParameter.HOME, AddressTypeParameter.PREF);

			f = it.next();
			assertEquals("item3", f.getGroup());
			assertEquals(null, f.getPoBox());
			assertEquals(null, f.getExtendedAddress());
			assertEquals("Street4" + NEWLINE + "Building 6" + NEWLINE + "Floor 8", f.getStreetAddress());
			assertEquals("New York", f.getLocality());
			assertEquals(null, f.getRegion());
			assertEquals("12345", f.getPostalCode());
			assertEquals("USA", f.getCountry());
			assertSetEquals(f.getTypes(), AddressTypeParameter.WORK);

			assertFalse(it.hasNext());
		}

		//NOTE
		{
			Iterator<NoteType> it = vcard.getNotes().iterator();

			NoteType f = it.next();
			assertEquals("THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE." + NEWLINE + "Favotire Color: Blue", f.getValue());

			assertFalse(it.hasNext());
		}

		//URL
		{
			Iterator<UrlType> it = vcard.getUrls().iterator();

			UrlType f = it.next();
			assertEquals("item4", f.getGroup());
			assertEquals("http://www.ibm.com", f.getValue());
			assertEquals("pref", f.getType());

			assertFalse(it.hasNext());
		}

		//BDAY
		{
			BirthdayType f = vcard.getBirthday();
			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(Calendar.YEAR, 2012);
			c.set(Calendar.MONTH, Calendar.JUNE);
			c.set(Calendar.DAY_OF_MONTH, 6);
			assertEquals(c.getTime(), f.getDate());
		}

		//PHOTO
		{
			Iterator<PhotoType> it = vcard.getPhotos().iterator();

			PhotoType f = it.next();
			assertEquals(null, f.getContentType());
			assertEquals(18242, f.getData().length);

			assertFalse(it.hasNext());
		}

		//extended types
		{
			assertEquals(9, countExtTypes(vcard));

			RawType f = vcard.getExtendedTypes("X-PHONETIC-FIRST-NAME").get(0);
			assertEquals("X-PHONETIC-FIRST-NAME", f.getPropertyName());
			assertEquals("Jon", f.getValue());

			f = vcard.getExtendedTypes("X-PHONETIC-LAST-NAME").get(0);
			assertEquals("X-PHONETIC-LAST-NAME", f.getPropertyName());
			assertEquals("Dow", f.getValue());

			f = vcard.getExtendedTypes("X-ABLABEL").get(0);
			assertEquals("X-ABLabel", f.getPropertyName());
			assertEquals("AssistantPhone", f.getValue());
			assertEquals("item1", f.getGroup());

			f = vcard.getExtendedTypes("X-ABADR").get(0);
			assertEquals("X-ABADR", f.getPropertyName());
			assertEquals("Silicon Alley", f.getValue());
			assertEquals("item2", f.getGroup());

			f = vcard.getExtendedTypes("X-ABADR").get(1);
			assertEquals("X-ABADR", f.getPropertyName());
			assertEquals("Street 4, Building 6,\\nFloor 8\\nNew York\\nUSA", f.getValue());
			assertEquals("item3", f.getGroup());

			f = vcard.getExtendedTypes("X-ABLABEL").get(1);
			assertEquals("X-ABLabel", f.getPropertyName());
			assertEquals("_$!<HomePage>!$_", f.getValue());
			assertEquals("item4", f.getGroup());

			f = vcard.getExtendedTypes("X-ABRELATEDNAMES").get(0);
			assertEquals("X-ABRELATEDNAMES", f.getPropertyName());
			assertEquals("Jenny", f.getValue());
			assertEquals("item5", f.getGroup());
			assertSetEquals(f.getSubTypes().getTypes(), "pref");

			f = vcard.getExtendedTypes("X-ABLABEL").get(2);
			assertEquals("X-ABLabel", f.getPropertyName());
			assertEquals("Spouse", f.getValue());
			assertEquals("item5", f.getGroup());

			f = vcard.getExtendedTypes("X-ABUID").get(0);
			assertEquals("X-ABUID", f.getPropertyName());
			assertEquals("6B29A774-D124-4822-B8D0-2780EC117F60\\:ABPerson", f.getValue());
		}

		assertValidate(vcard.validate(VCardVersion.V3_0), vcard.getEmails().get(0), vcard.getPhotos().get(0));
		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void outlook2003VCard() throws Throwable {
		VCardReader reader = new VCardReader(getClass().getResourceAsStream("outlook-2003.vcf"));
		VCard vcard = reader.readNext();

		//VERSION
		assertEquals(VCardVersion.V2_1, vcard.getVersion());

		//N
		{
			StructuredNameType f = vcard.getStructuredName();
			assertEquals("Doe", f.getFamily());
			assertEquals("John", f.getGiven());
			assertTrue(f.getAdditional().isEmpty());
			assertEquals(Arrays.asList("Mr."), f.getPrefixes());
			assertEquals(Arrays.asList("III"), f.getSuffixes());
		}

		//FN
		{
			FormattedNameType f = vcard.getFormattedName();
			assertEquals("John Doe III", f.getValue());
		}

		//NICKNAME
		{
			NicknameType f = vcard.getNickname();
			assertEquals(Arrays.asList("Joey"), f.getValues());
		}

		//ORG
		{
			OrganizationType f = vcard.getOrganization();
			assertEquals(Arrays.asList("Company, The", "TheDepartment"), f.getValues());
		}

		//TITLE
		{
			Iterator<TitleType> it = vcard.getTitles().iterator();

			TitleType f = it.next();
			assertEquals("The Job Title", f.getValue());

			assertFalse(it.hasNext());
		}

		//NOTE
		{
			Iterator<NoteType> it = vcard.getNotes().iterator();

			NoteType f = it.next();
			assertEquals("This is the note field!!\r\nSecond line\r\n\r\nThird line is empty\r\n", f.getValue());

			assertFalse(it.hasNext());
		}

		//TEL
		{
			Iterator<TelephoneType> it = vcard.getTelephoneNumbers().iterator();
			TelephoneType f = it.next();

			assertEquals("BusinessPhone", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.VOICE);

			f = it.next();
			assertEquals("HomePhone", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.HOME, TelephoneTypeParameter.VOICE);

			f = it.next();
			assertEquals("MobilePhone", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.CELL, TelephoneTypeParameter.VOICE);

			f = it.next();
			assertEquals("BusinessFaxPhone", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.FAX, TelephoneTypeParameter.WORK);

			assertFalse(it.hasNext());
		}

		//ADR
		{
			Iterator<AddressType> it = vcard.getAddresses().iterator();

			AddressType f = it.next();
			assertEquals(null, f.getPoBox());
			assertEquals("TheOffice", f.getExtendedAddress());
			assertEquals("123 Main St", f.getStreetAddress());
			assertEquals("Austin", f.getLocality());
			assertEquals("TX", f.getRegion());
			assertEquals("12345", f.getPostalCode());
			assertEquals("United States of America", f.getCountry());
			assertEquals("TheOffice\r\n123 Main St\r\nAustin, TX 12345\r\nUnited States of America", f.getLabel());
			assertSetEquals(f.getTypes(), AddressTypeParameter.WORK);

			assertFalse(it.hasNext());
		}

		//LABEL
		{
			assertTrue(vcard.getOrphanedLabels().isEmpty());
		}

		//URL
		{
			Iterator<UrlType> it = vcard.getUrls().iterator();

			UrlType f = it.next();
			assertEquals("http://web-page-address.com", f.getValue());
			assertEquals("WORK", f.getType());

			assertFalse(it.hasNext());
		}

		//ROLE
		{
			Iterator<RoleType> it = vcard.getRoles().iterator();

			RoleType f = it.next();
			assertEquals("TheProfession", f.getValue());

			assertFalse(it.hasNext());
		}

		//BDAY
		{
			BirthdayType f = vcard.getBirthday();
			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(Calendar.YEAR, 1980);
			c.set(Calendar.MONTH, Calendar.MARCH);
			c.set(Calendar.DAY_OF_MONTH, 21);
			assertEquals(c.getTime(), f.getDate());
		}

		//KEY
		{
			Iterator<KeyType> it = vcard.getKeys().iterator();

			KeyType f = it.next();
			assertEquals(KeyTypeParameter.X509, f.getContentType());
			assertEquals(805, f.getData().length);

			assertFalse(it.hasNext());
		}

		//EMAIL
		{
			Iterator<EmailType> it = vcard.getEmails().iterator();

			EmailType f = it.next();
			assertEquals("jdoe@hotmail.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.PREF, EmailTypeParameter.INTERNET);

			assertFalse(it.hasNext());
		}

		//FBURL
		{
			Iterator<FbUrlType> it = vcard.getFbUrls().iterator();

			//Outlook 2003 apparently doesn't output FBURL correctly:
			//http://help.lockergnome.com/office/BUG-Outlook-2003-exports-FBURL-vCard-incorrectly--ftopict423660.html
			FbUrlType f = it.next();
			assertEquals("????????????????s????????????" + (char) 12, f.getValue());

			assertFalse(it.hasNext());
		}

		//REV
		{
			RevisionType f = vcard.getRevision();
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			c.clear();
			c.set(Calendar.YEAR, 2012);
			c.set(Calendar.MONTH, Calendar.OCTOBER);
			c.set(Calendar.DAY_OF_MONTH, 12);
			c.set(Calendar.HOUR_OF_DAY, 21);
			c.set(Calendar.MINUTE, 5);
			c.set(Calendar.SECOND, 25);
			assertEquals(c.getTime(), f.getValue());
		}

		assertValidate(vcard.validate(VCardVersion.V2_1), vcard.getNickname(), vcard.getFbUrls().get(0));
		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void thunderbird() throws Throwable {
		VCardReader reader = new VCardReader(getClass().getResourceAsStream("thunderbird-MoreFunctionsForAddressBook-extension.vcf"));
		VCard vcard = reader.readNext();

		//VERSION
		assertEquals(VCardVersion.V3_0, vcard.getVersion());

		//N
		{
			StructuredNameType f = vcard.getStructuredName();
			assertEquals("Doe", f.getFamily());
			assertEquals("John", f.getGiven());
			assertTrue(f.getAdditional().isEmpty());
			assertTrue(f.getPrefixes().isEmpty());
			assertTrue(f.getSuffixes().isEmpty());
		}

		//FN
		{
			FormattedNameType f = vcard.getFormattedName();
			assertEquals("John Doe", f.getValue());
		}

		//ORG
		{
			OrganizationType f = vcard.getOrganization();
			assertEquals(Arrays.asList("TheOrganization", "TheDepartment"), f.getValues());
		}

		//NICKNAME
		{
			NicknameType f = vcard.getNickname();
			assertEquals(Arrays.asList("Johnny"), f.getValues());
		}

		//ADR
		{
			Iterator<AddressType> it = vcard.getAddresses().iterator();

			AddressType f = it.next();
			assertEquals(null, f.getPoBox());
			assertEquals("222 Broadway", f.getExtendedAddress());
			assertEquals("Suite 100", f.getStreetAddress());
			assertEquals("New York", f.getLocality());
			assertEquals("NY", f.getRegion());
			assertEquals("98765", f.getPostalCode());
			assertEquals("USA", f.getCountry());
			assertSetEquals(f.getTypes(), AddressTypeParameter.WORK, AddressTypeParameter.POSTAL);

			f = it.next();
			assertEquals(null, f.getPoBox());
			assertEquals("123 Main St", f.getExtendedAddress());
			assertEquals("Apt 10", f.getStreetAddress());
			assertEquals("Austin", f.getLocality());
			assertEquals("TX", f.getRegion());
			assertEquals("12345", f.getPostalCode());
			assertEquals("USA", f.getCountry());
			assertSetEquals(f.getTypes(), AddressTypeParameter.HOME, AddressTypeParameter.POSTAL);

			assertFalse(it.hasNext());
		}

		//TEL
		{
			Iterator<TelephoneType> it = vcard.getTelephoneNumbers().iterator();

			TelephoneType f = it.next();
			assertEquals("555-555-1111", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.VOICE);

			f = it.next();
			assertEquals("555-555-2222", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.HOME, TelephoneTypeParameter.VOICE);

			f = it.next();
			assertEquals("555-555-5555", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.CELL, TelephoneTypeParameter.VOICE);

			f = it.next();
			assertEquals("555-555-3333", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.FAX);

			f = it.next();
			assertEquals("555-555-4444", f.getText());
			assertSetEquals(f.getTypes(), TelephoneTypeParameter.PAGER);

			assertFalse(it.hasNext());
		}

		//EMAIL
		{
			Iterator<EmailType> it = vcard.getEmails().iterator();

			EmailType f = it.next();
			assertEquals("doe.john@hotmail.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.INTERNET, EmailTypeParameter.PREF);

			f = it.next();
			assertEquals("additional-email@company.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.INTERNET);

			f = it.next();
			assertEquals("additional-email1@company.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.INTERNET);

			f = it.next();
			assertEquals("additional-email2@company.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.INTERNET);

			f = it.next();
			assertEquals("additional-email3@company.com", f.getValue());
			assertSetEquals(f.getTypes(), EmailTypeParameter.INTERNET);

			assertFalse(it.hasNext());
		}

		//URL
		{
			Iterator<UrlType> it = vcard.getUrls().iterator();

			UrlType f = it.next();
			assertEquals("http://www.private-webpage.com", f.getValue());
			assertEquals("HOME", f.getType());

			f = it.next();
			assertEquals("http://www.work-webpage.com", f.getValue());
			assertEquals("WORK", f.getType());

			assertFalse(it.hasNext());
		}

		//TITLE
		{
			Iterator<TitleType> it = vcard.getTitles().iterator();

			TitleType f = it.next();
			assertEquals("TheTitle", f.getValue());

			assertFalse(it.hasNext());
		}

		//CATEGORIES
		{
			//commas are incorrectly escaped, so there is only 1 item
			CategoriesType f = vcard.getCategories();
			assertEquals(Arrays.asList("category1, category2, category3"), f.getValues());
		}

		//BDAY
		{
			BirthdayType f = vcard.getBirthday();
			Calendar c = Calendar.getInstance();
			c.clear();
			c.set(Calendar.YEAR, 1970);
			c.set(Calendar.MONTH, Calendar.SEPTEMBER);
			c.set(Calendar.DAY_OF_MONTH, 21);
			assertEquals(c.getTime(), f.getDate());
		}

		//NOTE
		{
			Iterator<NoteType> it = vcard.getNotes().iterator();

			NoteType f = it.next();
			assertEquals("This is the notes field." + NEWLINE + "Second Line" + NEWLINE + NEWLINE + "Fourth Line" + NEWLINE + "You can put anything in the \"note\" field; even curse words.", f.getValue());

			assertFalse(it.hasNext());
		}

		//PHOTO
		{
			Iterator<PhotoType> it = vcard.getPhotos().iterator();

			PhotoType f = it.next();
			assertEquals(ImageTypeParameter.JPEG, f.getContentType());
			assertEquals(8940, f.getData().length);

			assertFalse(it.hasNext());
		}

		//extended types
		{
			assertEquals(2, countExtTypes(vcard));

			RawType f = vcard.getExtendedTypes("X-SPOUSE").get(0);
			assertEquals("X-SPOUSE", f.getPropertyName());
			assertEquals("TheSpouse", f.getValue());

			f = vcard.getExtendedTypes("X-ANNIVERSARY").get(0);
			assertEquals("X-ANNIVERSARY", f.getPropertyName());
			assertEquals("1990-04-30", f.getValue());
		}

		assertValidate(vcard.validate(VCardVersion.V3_0), vcard.getStructuredName(), vcard.getFormattedName(), vcard.getOrganization(), vcard.getNickname(), vcard.getAddresses().get(0), vcard.getAddresses().get(1), vcard.getTitles().get(0), vcard.getCategories(), vcard.getNotes().get(0));
		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void rfc6350_example() throws Throwable {
		VCardReader reader = new VCardReader(getClass().getResourceAsStream("rfc6350-example.vcf"));
		VCard vcard = reader.readNext();

		assertEquals(VCardVersion.V4_0, vcard.getVersion());
		assertEquals(16, vcard.getAllTypes().size());

		assertEquals("Simon Perreault", vcard.getFormattedName().getValue());

		StructuredNameType n = vcard.getStructuredName();
		assertEquals("Perreault", n.getFamily());
		assertEquals("Simon", n.getGiven());
		assertEquals(Arrays.asList(), n.getAdditional());
		assertEquals(Arrays.asList(), n.getPrefixes());
		assertEquals(Arrays.asList("ing. jr", "M.Sc."), n.getSuffixes());

		PartialDate expectedBday = PartialDate.date(null, 2, 3);
		PartialDate actualBday = vcard.getBirthday().getPartialDate();
		assertEquals(expectedBday, actualBday);

		PartialDate expectedAnniversary = PartialDate.dateTime(2009, 8, 8, 14, 30, null, new UtcOffset(-5, 0));
		PartialDate actualAnniversary = vcard.getAnniversary().getPartialDate();
		assertEquals(expectedAnniversary, actualAnniversary);

		assertTrue(vcard.getGender().isMale());

		LanguageType lang = vcard.getLanguages().get(0);
		assertEquals("fr", lang.getValue());
		assertIntEquals(1, lang.getPref());

		lang = vcard.getLanguages().get(1);
		assertEquals("en", lang.getValue());
		assertIntEquals(2, lang.getPref());

		OrganizationType org = vcard.getOrganization();
		assertEquals(Arrays.asList("Viagenie"), org.getValues());
		assertEquals("work", org.getType());

		AddressType adr = vcard.getAddresses().get(0);
		assertNull(adr.getPoBox());
		assertEquals("Suite D2-630", adr.getExtendedAddress());
		assertEquals("2875 Laurier", adr.getStreetAddress());
		assertEquals("Quebec", adr.getLocality());
		assertEquals("QC", adr.getRegion());
		assertEquals("G1V 2M2", adr.getPostalCode());
		assertEquals("Canada", adr.getCountry());
		assertSetEquals(adr.getTypes(), AddressTypeParameter.WORK);

		TelephoneType tel = vcard.getTelephoneNumbers().get(0);
		TelUri expectedUri = new TelUri.Builder("+1-418-656-9254").extension("102").build();
		assertEquals(expectedUri, tel.getUri());
		assertSetEquals(tel.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.VOICE);
		assertIntEquals(1, tel.getPref());

		tel = vcard.getTelephoneNumbers().get(1);
		expectedUri = new TelUri.Builder("+1-418-262-6501").build();
		assertEquals(expectedUri, tel.getUri());
		assertSetEquals(tel.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.VOICE, TelephoneTypeParameter.CELL, TelephoneTypeParameter.VIDEO, TelephoneTypeParameter.TEXT);

		EmailType email = vcard.getEmails().get(0);
		assertEquals("simon.perreault@viagenie.ca", email.getValue());
		assertSetEquals(email.getTypes(), EmailTypeParameter.WORK);

		GeoType geo = vcard.getGeo();
		assertEquals(Double.valueOf(46.772673), geo.getLatitude());
		assertEquals(Double.valueOf(-71.282945), geo.getLongitude());
		assertEquals("work", geo.getType());

		KeyType key = vcard.getKeys().get(0);
		assertEquals("http://www.viagenie.ca/simon.perreault/simon.asc", key.getUrl());
		assertEquals("work", key.getType());

		TimezoneType tz = vcard.getTimezone();
		assertIntEquals(-5, tz.getHourOffset());
		assertIntEquals(0, tz.getMinuteOffset());

		UrlType url = vcard.getUrls().get(0);
		assertEquals("http://nomis80.org", url.getValue());
		assertEquals("home", url.getType());

		assertValidate(vcard.validate(VCardVersion.V4_0));
		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void rfc2426_example() throws Throwable {
		VCardReader reader = new VCardReader(getClass().getResourceAsStream("rfc2426-example.vcf"));

		{
			VCard vcard = reader.readNext();

			assertEquals(VCardVersion.V3_0, vcard.getVersion());
			assertEquals(8, vcard.getAllTypes().size());

			assertEquals("Frank Dawson", vcard.getFormattedName().getValue());

			assertEquals(Arrays.asList("Lotus Development Corporation"), vcard.getOrganization().getValues());

			AddressType adr = vcard.getAddresses().get(0);
			assertNull(adr.getPoBox());
			assertNull(adr.getExtendedAddress());
			assertEquals("6544 Battleford Drive", adr.getStreetAddress());
			assertEquals("Raleigh", adr.getLocality());
			assertEquals("NC", adr.getRegion());
			assertEquals("27613-3502", adr.getPostalCode());
			assertEquals("U.S.A.", adr.getCountry());
			assertSetEquals(adr.getTypes(), AddressTypeParameter.WORK, AddressTypeParameter.POSTAL, AddressTypeParameter.PARCEL);

			TelephoneType tel = vcard.getTelephoneNumbers().get(0);
			assertEquals("+1-919-676-9515", tel.getText());
			assertSetEquals(tel.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.VOICE, TelephoneTypeParameter.MSG);

			tel = vcard.getTelephoneNumbers().get(1);
			assertEquals("+1-919-676-9564", tel.getText());
			assertSetEquals(tel.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.FAX);

			EmailType email = vcard.getEmails().get(0);
			assertEquals("Frank_Dawson@Lotus.com", email.getValue());
			assertSetEquals(email.getTypes(), EmailTypeParameter.INTERNET, EmailTypeParameter.PREF);

			email = vcard.getEmails().get(1);
			assertEquals("fdawson@earthlink.net", email.getValue());
			assertSetEquals(email.getTypes(), EmailTypeParameter.INTERNET);

			assertEquals("http://home.earthlink.net/~fdawson", vcard.getUrls().get(0).getValue());

			assertValidate(vcard.validate(VCardVersion.V3_0), (VCardType) null);
			assertWarnings(0, reader.getWarnings());
		}

		{
			VCard vcard = reader.readNext();

			assertEquals(VCardVersion.V3_0, vcard.getVersion());
			assertEquals(6, vcard.getAllTypes().size());

			assertEquals("Tim Howes", vcard.getFormattedName().getValue());

			assertEquals(Arrays.asList("Netscape Communications Corp."), vcard.getOrganization().getValues());

			AddressType adr = vcard.getAddresses().get(0);
			assertNull(adr.getPoBox());
			assertNull(adr.getExtendedAddress());
			assertEquals("501 E. Middlefield Rd.", adr.getStreetAddress());
			assertEquals("Mountain View", adr.getLocality());
			assertEquals("CA", adr.getRegion());
			assertEquals("94043", adr.getPostalCode());
			assertEquals("U.S.A.", adr.getCountry());
			assertSetEquals(adr.getTypes(), AddressTypeParameter.WORK);

			TelephoneType tel = vcard.getTelephoneNumbers().get(0);
			assertEquals("+1-415-937-3419", tel.getText());
			assertSetEquals(tel.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.VOICE, TelephoneTypeParameter.MSG);

			tel = vcard.getTelephoneNumbers().get(1);
			assertEquals("+1-415-528-4164", tel.getText());
			assertSetEquals(tel.getTypes(), TelephoneTypeParameter.WORK, TelephoneTypeParameter.FAX);

			EmailType email = vcard.getEmails().get(0);
			assertEquals("howes@netscape.com", email.getValue());
			assertSetEquals(email.getTypes(), EmailTypeParameter.INTERNET);

			assertValidate(vcard.validate(VCardVersion.V3_0), (VCardType) null);
			assertWarnings(0, reader.getWarnings());
		}

		assertNull(reader.readNext());
	}

	/**
	 * Counts the number of extended types in a vCard.
	 * @param vcard the vCard
	 * @return the number of extended types
	 */
	private int countExtTypes(VCard vcard) {
		return vcard.getExtendedTypes().size();
	}
}