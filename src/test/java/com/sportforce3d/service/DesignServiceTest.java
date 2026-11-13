package com.sportforce3d.service;

import com.sportforce3d.db.TestDatabases;
import com.sportforce3d.design.DesignDefaults;
import com.sportforce3d.design.DesignDocument;
import com.sportforce3d.design.DraftStore;
import com.sportforce3d.design.SampleDesigns;
import com.sportforce3d.model.DesignAsset;
import com.sportforce3d.model.DesignSummary;
import com.sportforce3d.repository.DesignRepository;
import com.sportforce3d.repository.UserRepository;
import com.sportforce3d.storage.StorageService;
import com.sportforce3d.three.model.Zone;
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

    private AuthService auth;
    private DesignService designs;
    private DesignRepository repo;
    private StorageService storage;
    private Path front;
    private Path back;

    @BeforeEach
    void setUp() throws Exception {
        var db = TestDatabases.migrated(tempDir);
        Session session = new Session();
        auth = new AuthService(new UserRepository(db), new PasswordHasher(4), session,
                new LoginThrottle(Clock.systemUTC()), Clock.systemUTC());
        repo = new DesignRepository(db);
        storage = new StorageService(tempDir.resolve("storage"));
        designs = new DesignService(repo, storage, new DraftStore(tempDir.resolve("drafts")), session, Clock.systemUTC());
        front = png(tempDir.resolve("front.png"));
        back = png(tempDir.resolve("back.png"));
        auth.login("cliente@sportforce3d.pe", "Cliente123!".toCharArray());
    }

    private static Path png(Path p) throws Exception {
        Files.write(p, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3});
        return p;
    }

    @Test
    void savesVersionsWithThumbnailsAndTechnicalElements() throws Exception {
        DesignDocument doc = SampleDesigns.halcones();
        DesignSummary first = designs.save(null, "Los Halcones", doc, front, back);
        assertThat(first.version()).isEqualTo(1);
        assertThat(Files.exists(storage.resolve(first.thumbnailFront()))).isTrue();
        assertThat(repo.countElements(first.id())).isEqualTo(doc.elements().size());

        DesignDocument changed = doc.withColor(Zone.JERSEY_BODY, "#0E7C3A");
        DesignSummary second = designs.save(first.id(), "Los Halcones (verde)", changed, png(tempDir.resolve("f2.png")),
                png(tempDir.resolve("b2.png")));
        assertThat(second.version()).isEqualTo(2);
        assertThat(second.name()).isEqualTo("Los Halcones (verde)");
        assertThat(Files.exists(storage.resolve(first.thumbnailFront()))).as("la miniatura anterior se borra").isFalse();
        assertThat(designs.open(first.id()).document()).isEqualTo(changed);
        assertThat(designs.myDesigns()).singleElement().extracting(DesignSummary::id).isEqualTo(first.id());
    }

    @Test
    void validatesBeforeSaving() {
        DesignDocument bad = DesignDefaults.document(com.sportforce3d.three.model.BodyType.ADULTO_HOMBRE)
                .addElement(DesignDefaults.backNumber("123", "#FFFFFF", "#000000"));
        assertThatThrownBy(() -> designs.save(null, "Malo", bad, null, null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> designs.save(null, "  ", SampleDesigns.halcones(), null, null))
                .isInstanceOf(ValidationException.class);
        assertThat(designs.myDesigns()).isEmpty();
    }

    @Test
    void duplicateAndDeleteOnlyOwnDesigns() {
        DesignSummary mine = designs.save(null, "Mío", SampleDesigns.halcones(), front, back);
        DesignSummary copy = designs.duplicate(mine.id());
        assertThat(copy.name()).isEqualTo("Copia de Mío");
        assertThat(copy.thumbnailFront()).isNotEqualTo(mine.thumbnailFront());
        assertThat(designs.myDesigns()).hasSize(2);

        auth.logout();
        auth.register("Otra Persona", "otra@correo.pe", null, "Futbol2026".toCharArray(), "Futbol2026".toCharArray());
        assertThatThrownBy(() -> designs.open(mine.id())).hasMessage("No encontramos ese diseño.");
        assertThatThrownBy(() -> designs.delete(mine.id())).hasMessage("No encontramos ese diseño.");
        assertThatThrownBy(() -> designs.save(mine.id(), "Robado", SampleDesigns.halcones(), null, null))
                .hasMessage("No encontramos ese diseño.");

        auth.logout();
        auth.login("cliente@sportforce3d.pe", "Cliente123!".toCharArray());
        designs.delete(copy.id());
        assertThat(designs.myDesigns()).extracting(DesignSummary::id).containsExactly(mine.id());

        auth.logout();
        auth.login("admin@sportforce3d.pe", "Admin123!".toCharArray());
        assertThat(designs.open(mine.id()).summary().name()).isEqualTo("Mío");
        assertThatThrownBy(() -> designs.delete(mine.id())).isInstanceOf(ServiceException.class);
    }

    @Test
    void logosNeedRightsDeclarationValidTypeAndOwnership() throws Exception {
        Path logo = png(tempDir.resolve("escudo.png"));
        assertThatThrownBy(() -> designs.uploadLogo(logo, false)).hasMessageContaining("derecho");
        Path fake = tempDir.resolve("virus.svg");
        Files.writeString(fake, "no soy un svg");
        assertThatThrownBy(() -> designs.uploadLogo(fake, true)).isInstanceOf(ValidationException.class);

        DesignAsset asset = designs.uploadLogo(logo, true);
        assertThat(asset.mimeType()).isEqualTo("image/png");
        assertThat(designs.uploadLogo(logo, true).id()).as("el mismo archivo no se duplica").isEqualTo(asset.id());

        Path svg = tempDir.resolve("escudo.svg");
        Files.writeString(svg, "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\"><circle cx=\"5\" cy=\"5\" r=\"4\" fill=\"#ff0000\"/></svg>");
        assertThat(designs.uploadLogo(svg, true).mimeType()).isEqualTo("image/svg+xml");
        assertThat(designs.myLogos()).hasSize(2);

        DesignDocument withLogo = SampleDesigns.halcones().addElement(DesignDefaults.logo(asset.id(), asset.path()));
        assertThat(designs.save(null, "Con logo", withLogo, null, null).id()).isPositive();

        auth.logout();
        auth.register("Otra Persona", "otra2@correo.pe", null, "Futbol2026".toCharArray(), "Futbol2026".toCharArray());
        assertThatThrownBy(() -> designs.save(null, "Logo ajeno", withLogo, null, null))
                .hasMessageContaining("no es tuyo");
    }

    @Test
    void draftIsKeptPerUserAndClearedOnSave() {
        DesignDocument doc = SampleDesigns.halcones();
        designs.saveDraft(null, "Borrador", doc);
        assertThat(designs.draft()).hasValueSatisfying(d -> {
            assertThat(d.name()).isEqualTo("Borrador");
            assertThat(com.sportforce3d.design.DesignJson.read(d.documentJson())).isEqualTo(doc);
        });
        designs.save(null, "Guardado", doc, null, null);
        assertThat(designs.draft()).isEmpty();
    }
}
