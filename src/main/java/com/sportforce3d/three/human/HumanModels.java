package com.sportforce3d.three.human;

import com.sportforce3d.design.GarmentCut;
import com.sportforce3d.three.model.ModelProviders;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HumanModels {

    private static final Logger log = LoggerFactory.getLogger(HumanModels.class);
    private static final int CACHE_SIZE = 2;
    private static final double MIN_SPECULAR = 0.08;

    private static final Map<Persona, HumanModel> CACHE = new LinkedHashMap<>(4, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Persona, HumanModel> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private record DressKey(Persona persona, GarmentCut cut) {
    }

    private static final Map<DressKey, OutfitFitter.DressedPerson> DRESSED = new LinkedHashMap<>(4, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<DressKey, OutfitFitter.DressedPerson> eldest) {
            return size() > CACHE_SIZE + 1;
        }
    };

    private HumanModels() {
    }

    public static OutfitFitter.DressedPerson dress(Persona persona) {
        return dress(persona, GarmentCut.DEFAULT);
    }

    public static OutfitFitter.DressedPerson dress(Persona persona, GarmentCut cut) {
        DressKey key = new DressKey(persona, cut);
        synchronized (DRESSED) {
            OutfitFitter.DressedPerson cached = DRESSED.get(key);
            if (cached != null) {
                return cached;
            }
        }
        HumanModel human = load(persona);
        long start = System.nanoTime();
        var simulated = SimulatedGarments.load(persona, cut);
        OutfitFitter.DressedPerson dressed = OutfitFitter.fit(human, ModelProviders.load(persona.fallback()), simulated);
        log.info("Uniforme ajustado a {} en {} ms (tela simulada: {}; {})", persona.key(),
                (System.nanoTime() - start) / 1_000_000, simulated.keySet(), cut.summary());
        synchronized (DRESSED) {
            DRESSED.put(key, dressed);
        }
        return dressed;
    }

    public static boolean available(Persona persona) {
        return persona != null && HumanModels.class.getResource(persona.objResource()) != null;
    }

    public static ObjMtlReader.Result readGeometry(Persona persona) {
        try (InputStream in = HumanModels.class.getResourceAsStream(persona.objResource())) {
            if (in == null) {
                throw new IllegalStateException("No existe " + persona.objResource());
            }
            return ObjMtlReader.read(new InputStreamReader(in, StandardCharsets.UTF_8), path -> open(persona, path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static HumanModel load(Persona persona) {
        synchronized (CACHE) {
            HumanModel cached = CACHE.get(persona);
            if (cached != null) {
                return cached;
            }
        }
        long start = System.nanoTime();
        ObjMtlReader.Result geometry = readGeometry(persona);
        Map<String, Texture> textures = new HashMap<>();
        List<HumanModel.Part> opaque = new ArrayList<>();
        List<HumanModel.Part> transparent = new ArrayList<>();
        for (ObjMtlReader.Group g : geometry.groups()) {
            ObjMtlReader.Material m = geometry.material(g.material());
            Texture tex = m.diffuseMap() == null ? null
                    : textures.computeIfAbsent(m.diffuseMap(), p -> texture(persona, p));
            boolean alpha = tex != null && tex.alpha();
            double ks = Math.max(MIN_SPECULAR, avg(m.specular()));
            HumanModel.Part part = new HumanModel.Part(g.material(), g.mesh(),
                    Color.color(m.diffuse()[0], m.diffuse()[1], m.diffuse()[2]), Color.gray(Math.min(1, ks)),
                    m.shininess() > 0 ? m.shininess() : 24, tex == null ? null : tex.image(), alpha);
            (alpha ? transparent : opaque).add(part);
        }
        List<HumanModel.Part> parts = new ArrayList<>(opaque);
        parts.addAll(transparent);
        HumanModel model = new HumanModel(persona, parts, geometry.min()[1], geometry.height());
        log.info("Persona 3D {} cargada en {} ms ({} materiales)", persona.key(),
                (System.nanoTime() - start) / 1_000_000, parts.size());
        synchronized (CACHE) {
            CACHE.put(persona, model);
        }
        return model;
    }

    private record Texture(Image image, boolean alpha) {
    }

    private static Texture texture(Persona persona, String path) {
        try (InputStream in = HumanModels.class.getResourceAsStream(persona.folder() + path)) {
            if (in == null) {
                log.warn("Falta la textura {}{}", persona.folder(), path);
                return null;
            }
            byte[] bytes = in.readAllBytes();
            Image image = new Image(new ByteArrayInputStream(bytes));
            if (image.isError()) {
                log.warn("No se pudo leer la textura {}{}", persona.folder(), path, image.getException());
                return null;
            }
            return new Texture(image, pngHasAlpha(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static boolean pngHasAlpha(byte[] png) {
        if (png.length < 33 || (png[0] & 0xFF) != 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') {
            return false;
        }
        int colorType = png[25] & 0xFF;
        if (colorType == 4 || colorType == 6) {
            return true;
        }
        int pos = 8;
        while (pos + 8 <= png.length) {
            int len = ((png[pos] & 0xFF) << 24) | ((png[pos + 1] & 0xFF) << 16) | ((png[pos + 2] & 0xFF) << 8)
                    | (png[pos + 3] & 0xFF);
            String type = new String(png, pos + 4, 4, StandardCharsets.US_ASCII);
            if (type.equals("tRNS")) {
                return true;
            }
            if (type.equals("IDAT") || len < 0) {
                return false;
            }
            pos += 12 + len;
        }
        return false;
    }

    private static Reader open(Persona persona, String path) {
        InputStream in = HumanModels.class.getResourceAsStream(persona.folder() + path);
        return in == null ? null : new InputStreamReader(in, StandardCharsets.UTF_8);
    }

    private static double avg(double[] rgb) {
        return (rgb[0] + rgb[1] + rgb[2]) / 3;
    }
}
