package com.sportforce3d.service;

import com.sportforce3d.design.DesignDocument;
import com.sportforce3d.design.DesignElement;
import com.sportforce3d.design.DesignJson;
import com.sportforce3d.design.DesignRules;
import com.sportforce3d.design.DraftStore;
import com.sportforce3d.design.ElementType;
import com.sportforce3d.model.DesignAsset;
import com.sportforce3d.model.DesignSummary;
import com.sportforce3d.model.Role;
import com.sportforce3d.model.User;
import com.sportforce3d.repository.DesignRepository;
import com.sportforce3d.repository.DesignRepository.StoredDesign;
import com.sportforce3d.storage.StorageService;
import com.sportforce3d.util.Validators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Diseños del cliente: guardar (con versión y capturas), abrir, duplicar, eliminar, logos subidos y borrador
 * local. Cada diseño pertenece a un usuario; solo su dueño (o un ADMIN, para revisar pedidos) puede abrirlo.
 */
public class DesignService {

    private static final Logger log = LoggerFactory.getLogger(DesignService.class);

    /** Diseño abierto: datos guardados y documento. */
    public record OpenedDesign(DesignSummary summary, DesignDocument document) {
    }

    private final DesignRepository designs;
    private final StorageService storage;
    private final DraftStore drafts;
    private final Session session;
    private final Clock clock;

    public DesignService(DesignRepository designs, StorageService storage, DraftStore drafts, Session session,
                         Clock clock) {
        this.designs = designs;
        this.storage = storage;
        this.drafts = drafts;
        this.session = session;
        this.clock = clock;
    }

    /**
     * Guarda un diseño nuevo ({@code designId} null) o una versión nueva de uno existente. Las capturas frontal y
     * posterior (PNG) se copian al almacenamiento; las anteriores se borran.
     */
    public DesignSummary save(Long designId, String name, DesignDocument doc, Path frontPng, Path backPng) {
        User me = session.requireUser();
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty() || cleanName.length() > 80) {
            throw ValidationException.of("name", "Ponle un nombre al diseño (hasta 80 caracteres).");
        }
        List<String> problems = DesignRules.problems(doc);
        if (!problems.isEmpty()) {
            throw ValidationException.of("design", problems.get(0));
        }
        checkAssets(me, doc);
        StoredDesign existing = designId == null ? null : owned(me, designId, false);

        String front = frontPng == null ? null : storage.store(frontPng, "designs", Set.of(StorageService.Kind.PNG)).path();
        String back = backPng == null ? null : storage.store(backPng, "designs", Set.of(StorageService.Kind.PNG)).path();
        String json = DesignJson.write(doc);
        long id;
        try {
            if (existing == null) {
                id = designs.create(me.id(), doc.productId(), cleanName, doc.schemaVersion(), json, doc.elements(), front, back);
            } else {
                id = designId;
                designs.addVersion(designId, cleanName, doc.productId(), doc.schemaVersion(), json, doc.elements(), front, back);
                deleteFile(existing.summary().thumbnailFront());
                deleteFile(existing.summary().thumbnailBack());
            }
        } catch (RuntimeException e) {
            deleteFile(front);
            deleteFile(back);
            throw e;
        }
        drafts.delete(me.id());
        log.info("Usuario {} guardó el diseño {} ({} elementos)", me.id(), id, doc.elements().size());
        return designs.find(id).orElseThrow().summary();
    }

    public List<DesignSummary> myDesigns() {
        return designs.findByUser(session.requireUser().id());
    }

    public OpenedDesign open(long designId) {
        User me = session.requireUser();
        StoredDesign d = owned(me, designId, true);
        return new OpenedDesign(d.summary(), DesignJson.read(d.documentJson()));
    }

    public DesignSummary duplicate(long designId) {
        User me = session.requireUser();
        StoredDesign d = owned(me, designId, false);
        String front = copy(d.summary().thumbnailFront());
        String back = copy(d.summary().thumbnailBack());
        DesignDocument doc = DesignJson.read(d.documentJson());
        String name = ("Copia de " + d.summary().name());
        long id = designs.create(me.id(), d.summary().productId(), name.length() > 80 ? name.substring(0, 80) : name,
                doc.schemaVersion(), d.documentJson(), doc.elements(), front, back);
        return designs.find(id).orElseThrow().summary();
    }

    public void delete(long designId) {
        User me = session.requireUser();
        owned(me, designId, false);
        designs.softDelete(designId, clock.instant());
    }

    // ---------------------------------------------------------------- Logos

    /** Sube un logo (PNG o SVG, máx. 5 MB). El cliente debe declarar que tiene derecho a usarlo. */
    public DesignAsset uploadLogo(Path file, boolean rightsDeclared) {
        User me = session.requireUser();
        if (!rightsDeclared) {
            throw ValidationException.of("rights", "Debes declarar que tienes derecho a usar este logo.");
        }
        String sha = sha256(file);
        Optional<DesignAsset> same = designs.findAssetBySha(me.id(), sha);
        if (same.isPresent()) {
            return same.get();
        }
        StorageService.StoredFile stored = storage.store(file, "uploads",
                Set.of(StorageService.Kind.PNG, StorageService.Kind.SVG));
        if (stored.kind() == StorageService.Kind.SVG) {
            try (InputStream in = Files.newInputStream(storage.resolve(stored.path()))) {
                com.sportforce3d.three.SvgLoader.parse(in);
            } catch (IllegalArgumentException | IOException e) {
                storage.delete(stored.path());
                throw ValidationException.of("file", "Ese SVG usa funciones no compatibles. Súbelo como PNG.");
            }
        }
        String original = file.getFileName().toString();
        return designs.insertAsset(me.id(), original.length() > 120 ? original.substring(0, 120) : original,
                stored.path(), stored.kind().mimeType(), stored.sizeBytes(), sha);
    }

    public List<DesignAsset> myLogos() {
        return designs.findAssets(session.requireUser().id());
    }

    // ---------------------------------------------------------------- Borrador

    public void saveDraft(Long designId, String name, DesignDocument doc) {
        User me = session.requireUser();
        drafts.save(me.id(), new DraftStore.Draft(designId, name, DesignJson.write(doc), clock.instant().toString()));
    }

    public Optional<DraftStore.Draft> draft() {
        return session.current().flatMap(u -> drafts.load(u.id()));
    }

    public void discardDraft() {
        session.current().ifPresent(u -> drafts.delete(u.id()));
    }

    // ---------------------------------------------------------------- Ayudas

    private StoredDesign owned(User me, long designId, boolean adminCanRead) {
        StoredDesign d = designs.find(designId).filter(x -> !x.deleted())
                .orElseThrow(() -> new ServiceException("No encontramos ese diseño."));
        boolean admin = adminCanRead && me.hasRole(Role.ADMIN);
        if (d.userId() != me.id() && !admin) {
            throw new ServiceException("No encontramos ese diseño.");
        }
        return d;
    }

    private void checkAssets(User me, DesignDocument doc) {
        for (DesignElement e : doc.elements()) {
            if (e.type() == ElementType.LOGO) {
                var asset = designs.findAsset(e.assetId())
                        .orElseThrow(() -> ValidationException.of("design", "Un logo del diseño ya no existe."));
                if (asset.userId() != me.id() || !asset.asset().path().equals(e.assetPath())) {
                    throw ValidationException.of("design", "Un logo del diseño no es tuyo.");
                }
            }
        }
    }

    private String copy(String path) {
        if (path == null) {
            return null;
        }
        Path src = storage.resolve(path);
        if (!Files.isRegularFile(src)) {
            return null;
        }
        return storage.store(src, "designs", Set.of(StorageService.Kind.PNG)).path();
    }

    private void deleteFile(String path) {
        if (path != null) {
            storage.delete(path);
        }
    }

    static String sha256(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
