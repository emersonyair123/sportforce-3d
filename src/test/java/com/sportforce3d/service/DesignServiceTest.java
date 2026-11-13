package com.sportforce3d.service;

import com.sportforce3d.db.TestDatabases;
import com.sportforce3d.design.PgeyDesignDefaults;
import com.sportforce3d.design.PgeyDesignDocument;
import com.sportforce3d.design.PgeyDraftStore;
import com.sportforce3d.design.PgeySampleDesigns;
import com.sportforce3d.model.PgeyDesignAsset;
import com.sportforce3d.model.PgeyDesignSummary;
import com.sportforce3d.repository.HepyDesignRepository;
import com.sportforce3d.repository.HepyUserRepository;
import com.sportforce3d.storage.HepyStorageService;
import com.sportforce3d.three.model.PgeyZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DesignServiceTest {

    @TempDir
    Path tempDir;

    private PpqhAuthService auth;
    private PgeyDesignService designs;
    private HepyDesignRepository repo;
    private HepyStorageService storage;
    private Path front;
    private Path back;

    @BeforeEach
    void setUp() throws Exception {
        var db = TestDatabases.migrated(tempDir);
        PpqhSession session = new PpqhSession();
        auth = new PpqhAuthService(new HepyUserRepository(db), new PpqhPasswordHasher(4), session,
                new PpqhLoginThrottle(Clock.systemUTC()), Clock.systemUTC());
        repo = new HepyDesignRepository(db);
        storage = new HepyStorageService(tempDir.resolve("storage"));
        designs = new PgeyDesignService(repo, storage, new PgeyDraftStore(tempDir.resolve("drafts")), session, Clock.systemUTC());
        front = png(tempDir.resolve("front.png"));
        back = png(tempDir.resolve("back.png"));
        auth.ppqhLogin("cliente@sportforce3d.pe", "Cliente123!".toCharArray());
    }

    private static Path png(Path p) throws Exception {
        Files.write(p, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3});
        return p;
    }

    @Test
    void savesVersionsWithThumbnailsAndTechnicalElements() throws Exception {
        PgeyDesignDocument doc = PgeySampleDesigns.pgeyHalcones();
        PgeyDesignSummary first = designs.pgeySave(null, "Los Halcones", doc, front, back);
        assertThat(first.pgeyVersion()).isEqualTo(1);
        assertThat(Files.exists(storage.hepyResolve(first.pgeyThumbnailFront()))).isTrue();
        assertThat(repo.hepyCountElements(first.pgeyId())).isEqualTo(doc.pgeyElements().size());

        PgeyDesignDocument changed = doc.pgeyWithColor(PgeyZone.JERSEY_BODY, "#0E7C3A");
        PgeyDesignSummary second = designs.pgeySave(first.pgeyId(), "Los Halcones (verde)", changed, png(tempDir.resolve("f2.png")),
                png(tempDir.resolve("b2.png")));
        assertThat(second.pgeyVersion()).isEqualTo(2);
        assertThat(second.pgeyName()).isEqualTo("Los Halcones (verde)");
        assertThat(Files.exists(storage.hepyResolve(first.pgeyThumbnailFront()))).as("la miniatura anterior se borra").isFalse();
        assertThat(designs.pgeyOpen(first.pgeyId()).pgeyDocument()).isEqualTo(changed);
        assertThat(designs.pgeyMyDesigns()).singleElement().extracting(PgeyDesignSummary::pgeyId).isEqualTo(first.pgeyId());
    }

    @Test
    void validatesBeforeSaving() {
        PgeyDesignDocument bad = PgeyDesignDefaults.pgeyDocument(com.sportforce3d.three.model.PgeyBodyType.ADULTO_HOMBRE)
                .pgeyAddElement(PgeyDesignDefaults.pgeyBackNumber("123", "#FFFFFF", "#000000"));
        assertThatThrownBy(() -> designs.pgeySave(null, "Malo", bad, null, null)).isInstanceOf(PpqhValidationException.class);
        assertThatThrownBy(() -> designs.pgeySave(null, "  ", PgeySampleDesigns.pgeyHalcones(), null, null))
                .isInstanceOf(PpqhValidationException.class);
        assertThat(designs.pgeyMyDesigns()).isEmpty();
    }

    @Test
    void duplicateAndDeleteOnlyOwnDesigns() {
        PgeyDesignSummary mine = designs.pgeySave(null, "Mío", PgeySampleDesigns.pgeyHalcones(), front, back);
        PgeyDesignSummary copy = designs.pgeyDuplicate(mine.pgeyId());
        assertThat(copy.pgeyName()).isEqualTo("Copia de Mío");
        assertThat(copy.pgeyThumbnailFront()).isNotEqualTo(mine.pgeyThumbnailFront());
        assertThat(designs.pgeyMyDesigns()).hasSize(2);

        auth.ppqhLogout();
        auth.ppqhRegister("Otra Persona", "otra@correo.pe", null, "Futbol2026".toCharArray(), "Futbol2026".toCharArray());
        assertThatThrownBy(() -> designs.pgeyOpen(mine.pgeyId())).hasMessage("No encontramos ese diseño.");
        assertThatThrownBy(() -> designs.pgeyDelete(mine.pgeyId())).hasMessage("No encontramos ese diseño.");
        assertThatThrownBy(() -> designs.pgeySave(mine.pgeyId(), "Robado", PgeySampleDesigns.pgeyHalcones(), null, null))
                .hasMessage("No encontramos ese diseño.");

        auth.ppqhLogout();
        auth.ppqhLogin("cliente@sportforce3d.pe", "Cliente123!".toCharArray());
        designs.pgeyDelete(copy.pgeyId());
        assertThat(designs.pgeyMyDesigns()).extracting(PgeyDesignSummary::pgeyId).containsExactly(mine.pgeyId());

        auth.ppqhLogout();
        auth.ppqhLogin("admin@sportforce3d.pe", "Admin123!".toCharArray());
        assertThat(designs.pgeyOpen(mine.pgeyId()).pgeySummary().pgeyName()).isEqualTo("Mío");
        assertThatThrownBy(() -> designs.pgeyDelete(mine.pgeyId())).isInstanceOf(PpqhServiceException.class);
    }

    @Test
    void logosNeedRightsDeclarationValidTypeAndOwnership() throws Exception {
        Path logo = png(tempDir.resolve("escudo.png"));
        assertThatThrownBy(() -> designs.pgeyUploadLogo(logo, false)).hasMessageContaining("derecho");
        Path fake = tempDir.resolve("virus.svg");
        Files.writeString(fake, "no soy un svg");
        assertThatThrownBy(() -> designs.pgeyUploadLogo(fake, true)).isInstanceOf(PpqhValidationException.class);

        PgeyDesignAsset asset = designs.pgeyUploadLogo(logo, true);
        assertThat(asset.pgeyMimeType()).isEqualTo("image/png");
        assertThat(designs.pgeyUploadLogo(logo, true).pgeyId()).as("el mismo archivo no se duplica").isEqualTo(asset.pgeyId());

        Path svg = tempDir.resolve("escudo.svg");
        Files.writeString(svg, "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\"><circle cx=\"5\" cy=\"5\" r=\"4\" fill=\"#ff0000\"/></svg>");
        assertThat(designs.pgeyUploadLogo(svg, true).pgeyMimeType()).isEqualTo("image/svg+xml");
        assertThat(designs.pgeyMyLogos()).hasSize(2);

        PgeyDesignDocument withLogo = PgeySampleDesigns.pgeyHalcones().pgeyAddElement(PgeyDesignDefaults.pgeyLogo(asset.pgeyId(), asset.pgeyPath()));
        assertThat(designs.pgeySave(null, "Con logo", withLogo, null, null).pgeyId()).isPositive();

        auth.ppqhLogout();
        auth.ppqhRegister("Otra Persona", "otra2@correo.pe", null, "Futbol2026".toCharArray(), "Futbol2026".toCharArray());
        assertThatThrownBy(() -> designs.pgeySave(null, "Logo ajeno", withLogo, null, null))
                .hasMessageContaining("no es tuyo");
    }

    @Test
    void draftIsKeptPerUserAndClearedOnSave() {
        PgeyDesignDocument doc = PgeySampleDesigns.pgeyHalcones();
        designs.pgeySaveDraft(null, "Borrador", doc);
        assertThat(designs.pgeyDraft()).hasValueSatisfying(d -> {
            assertThat(d.pgeyName()).isEqualTo("Borrador");
            assertThat(com.sportforce3d.design.PgeyDesignJson.pgeyRead(d.pgeyDocumentJson())).isEqualTo(doc);
        });
        designs.pgeySave(null, "Guardado", doc, null, null);
        assertThat(designs.pgeyDraft()).isEmpty();
    }
}
