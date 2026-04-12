package io.github.dfauth.ta;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class ZipUnZipUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public record Price(double close, String code, String date, double high, int id, double low, String market, double open, int volume) {}

    public static Price[] prices(String zipped) {
        try {
            return OBJECT_MAPPER.readValue(decodeAndUnzip(zipped).getBytes(), Price[].class);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testIt() throws IOException {
        String prices = zipAndEncode(new ByteArrayInputStream(PRICES.getBytes()));
        assertEquals(PRICES, decodeAndUnzip(new ByteArrayInputStream(prices.getBytes())));
    }

    @Test
    public void encodeFromClipboard() {
        try {
            log.info("from clipboard: {}", zipAndEncode(new ByteArrayInputStream(((String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor)).getBytes())));
        } catch (UnsupportedFlavorException | IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public String zipAndEncode(InputStream stream) {
        return new String(Base64.getEncoder().encode(zip(stream)));
    }

    public static String decodeAndUnzip(String str) {
        return decodeAndUnzip(str.getBytes());
    }

    public static String decodeAndUnzip(byte[] bytes) {
        return decodeAndUnzip(new ByteArrayInputStream(bytes));
    }

    public static String decodeAndUnzip(InputStream stream) {
        try {
            return new String(unzip(new ByteArrayInputStream(Base64.getDecoder().decode(stream.readAllBytes()))));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static byte[] unzip(InputStream stream) {
        try {
            ZipInputStream zipStream = new ZipInputStream(stream);
            zipStream.getNextEntry();
            return zipStream.readAllBytes();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static byte[] zip(InputStream stream) {
        return zip("zipEntry", stream);
    }

    public static byte[] zip(String name, InputStream stream) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipStream = new ZipOutputStream(baos);
            zipStream.putNextEntry(new ZipEntry(name));
            zipStream.write(stream.readAllBytes());
            zipStream.flush();
            zipStream.close();
            return baos.toByteArray();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static final String BHP = "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";

    public static final String PRICES = """
[
  {
    "close": 36.37,
    "code": "BHP",
    "date": "2025-04-14",
    "high": 36.61,
    "id": 144742,
    "low": 36.11,
    "market": "ASX",
    "open": 36.2,
    "volume": 8037473
  },
  {
    "close": 36.5,
    "code": "BHP",
    "date": "2025-04-15",
    "high": 36.9,
    "id": 144743,
    "low": 36.36,
    "market": "ASX",
    "open": 36.78,
    "volume": 6240293
  },
  {
    "close": 36.07,
    "code": "BHP",
    "date": "2025-04-16",
    "high": 36.31,
    "id": 144744,
    "low": 35.93,
    "market": "ASX",
    "open": 36.2,
    "volume": 7432788
  },
  {
    "close": 36.48,
    "code": "BHP",
    "date": "2025-04-17",
    "high": 36.67,
    "id": 144745,
    "low": 35.97,
    "market": "ASX",
    "open": 36,
    "volume": 9007017
  },
  {
    "close": 36.51,
    "code": "BHP",
    "date": "2025-04-22",
    "high": 36.9,
    "id": 144746,
    "low": 36.3,
    "market": "ASX",
    "open": 36.39,
    "volume": 9273114
  },
  {
    "close": 37.72,
    "code": "BHP",
    "date": "2025-04-23",
    "high": 37.79,
    "id": 144747,
    "low": 37.31,
    "market": "ASX",
    "open": 37.69,
    "volume": 8921838
  },
  {
    "close": 38.06,
    "code": "BHP",
    "date": "2025-04-24",
    "high": 38.29,
    "id": 144748,
    "low": 37.93,
    "market": "ASX",
    "open": 38.02,
    "volume": 7571001
  },
  {
    "close": 37.66,
    "code": "BHP",
    "date": "2025-04-28",
    "high": 38.14,
    "id": 144749,
    "low": 37.47,
    "market": "ASX",
    "open": 38.14,
    "volume": 11283554
  },
  {
    "close": 38.19,
    "code": "BHP",
    "date": "2025-04-29",
    "high": 38.33,
    "id": 144750,
    "low": 37.68,
    "market": "ASX",
    "open": 37.77,
    "volume": 5967938
  },
  {
    "close": 38.19,
    "code": "BHP",
    "date": "2025-04-30",
    "high": 38.33,
    "id": 144751,
    "low": 37.85,
    "market": "ASX",
    "open": 38.02,
    "volume": 9696608
  },
  {
    "close": 37.83,
    "code": "BHP",
    "date": "2025-05-01",
    "high": 37.83,
    "id": 144752,
    "low": 37.26,
    "market": "ASX",
    "open": 37.45,
    "volume": 8185861
  },
  {
    "close": 38.08,
    "code": "BHP",
    "date": "2025-05-02",
    "high": 38.2,
    "id": 144753,
    "low": 37.32,
    "market": "ASX",
    "open": 37.52,
    "volume": 12128860
  },
  {
    "close": 37.75,
    "code": "BHP",
    "date": "2025-05-05",
    "high": 38.07,
    "id": 144754,
    "low": 37.63,
    "market": "ASX",
    "open": 38.07,
    "volume": 5715861
  },
  {
    "close": 37.6,
    "code": "BHP",
    "date": "2025-05-06",
    "high": 37.82,
    "id": 144755,
    "low": 37.39,
    "market": "ASX",
    "open": 37.71,
    "volume": 6260439
  },
  {
    "close": 37.93,
    "code": "BHP",
    "date": "2025-05-07",
    "high": 38.25,
    "id": 144756,
    "low": 37.76,
    "market": "ASX",
    "open": 37.85,
    "volume": 9692690
  },
  {
    "close": 37.92,
    "code": "BHP",
    "date": "2025-05-08",
    "high": 38.14,
    "id": 144757,
    "low": 37.49,
    "market": "ASX",
    "open": 37.5,
    "volume": 8208632
  },
  {
    "close": 37.54,
    "code": "BHP",
    "date": "2025-05-09",
    "high": 37.68,
    "id": 144758,
    "low": 37.37,
    "market": "ASX",
    "open": 37.42,
    "volume": 7266299
  },
  {
    "close": 38.4,
    "code": "BHP",
    "date": "2025-05-12",
    "high": 38.55,
    "id": 144759,
    "low": 37.96,
    "market": "ASX",
    "open": 38.17,
    "volume": 8799346
  },
  {
    "close": 39.21,
    "code": "BHP",
    "date": "2025-05-13",
    "high": 39.52,
    "id": 144760,
    "low": 39.02,
    "market": "ASX",
    "open": 39.21,
    "volume": 8917371
  },
  {
    "close": 39.45,
    "code": "BHP",
    "date": "2025-05-14",
    "high": 39.45,
    "id": 144761,
    "low": 38.9,
    "market": "ASX",
    "open": 39.4,
    "volume": 8897426
  },
  {
    "close": 39.19,
    "code": "BHP",
    "date": "2025-05-15",
    "high": 39.36,
    "id": 144762,
    "low": 38.96,
    "market": "ASX",
    "open": 39.35,
    "volume": 8679974
  },
  {
    "close": 39.72,
    "code": "BHP",
    "date": "2025-05-16",
    "high": 39.97,
    "id": 144763,
    "low": 39.37,
    "market": "ASX",
    "open": 39.43,
    "volume": 10433059
  },
  {
    "close": 38.75,
    "code": "BHP",
    "date": "2025-05-19",
    "high": 39.48,
    "id": 144764,
    "low": 38.75,
    "market": "ASX",
    "open": 39.3,
    "volume": 6741949
  },
  {
    "close": 38.6,
    "code": "BHP",
    "date": "2025-05-20",
    "high": 39.06,
    "id": 144765,
    "low": 38.55,
    "market": "ASX",
    "open": 39.03,
    "volume": 7068394
  },
  {
    "close": 38.65,
    "code": "BHP",
    "date": "2025-05-21",
    "high": 38.94,
    "id": 144766,
    "low": 38.32,
    "market": "ASX",
    "open": 38.73,
    "volume": 7003719
  },
  {
    "close": 38.63,
    "code": "BHP",
    "date": "2025-05-22",
    "high": 38.76,
    "id": 144767,
    "low": 38.45,
    "market": "ASX",
    "open": 38.57,
    "volume": 5995013
  },
  {
    "close": 38.35,
    "code": "BHP",
    "date": "2025-05-23",
    "high": 38.63,
    "id": 144768,
    "low": 38.33,
    "market": "ASX",
    "open": 38.41,
    "volume": 6094463
  },
  {
    "close": 38.57,
    "code": "BHP",
    "date": "2025-05-26",
    "high": 38.64,
    "id": 144769,
    "low": 38.23,
    "market": "ASX",
    "open": 38.6,
    "volume": 5254368
  },
  {
    "close": 38.64,
    "code": "BHP",
    "date": "2025-05-27",
    "high": 38.72,
    "id": 144770,
    "low": 38.19,
    "market": "ASX",
    "open": 38.57,
    "volume": 6446469
  },
  {
    "close": 38.45,
    "code": "BHP",
    "date": "2025-05-28",
    "high": 39.02,
    "id": 144771,
    "low": 38.27,
    "market": "ASX",
    "open": 38.65,
    "volume": 7462593
  },
  {
    "close": 38.15,
    "code": "BHP",
    "date": "2025-05-29",
    "high": 38.68,
    "id": 144772,
    "low": 38.04,
    "market": "ASX",
    "open": 38.6,
    "volume": 9337472
  },
  {
    "close": 38.25,
    "code": "BHP",
    "date": "2025-05-30",
    "high": 38.34,
    "id": 144773,
    "low": 38.04,
    "market": "ASX",
    "open": 38.09,
    "volume": 21266999
  },
  {
    "close": 37.78,
    "code": "BHP",
    "date": "2025-06-02",
    "high": 38.26,
    "id": 144774,
    "low": 37.65,
    "market": "ASX",
    "open": 38.1,
    "volume": 7279689
  },
  {
    "close": 37.56,
    "code": "BHP",
    "date": "2025-06-03",
    "high": 38.17,
    "id": 144775,
    "low": 37.56,
    "market": "ASX",
    "open": 38.03,
    "volume": 9092695
  },
  {
    "close": 37.95,
    "code": "BHP",
    "date": "2025-06-04",
    "high": 38.13,
    "id": 144776,
    "low": 37.61,
    "market": "ASX",
    "open": 37.63,
    "volume": 8472742
  },
  {
    "close": 37.98,
    "code": "BHP",
    "date": "2025-06-05",
    "high": 38.27,
    "id": 144777,
    "low": 37.84,
    "market": "ASX",
    "open": 37.89,
    "volume": 8451413
  },
  {
    "close": 38.23,
    "code": "BHP",
    "date": "2025-06-06",
    "high": 38.44,
    "id": 144778,
    "low": 38.14,
    "market": "ASX",
    "open": 38.2,
    "volume": 8368361
  },
  {
    "close": 38.48,
    "code": "BHP",
    "date": "2025-06-10",
    "high": 38.7,
    "id": 144779,
    "low": 38.19,
    "market": "ASX",
    "open": 38.44,
    "volume": 8423622
  },
  {
    "close": 39.05,
    "code": "BHP",
    "date": "2025-06-11",
    "high": 39.39,
    "id": 144780,
    "low": 39,
    "market": "ASX",
    "open": 39.11,
    "volume": 8321571
  },
  {
    "close": 38.34,
    "code": "BHP",
    "date": "2025-06-12",
    "high": 39,
    "id": 144781,
    "low": 38.34,
    "market": "ASX",
    "open": 38.99,
    "volume": 8853092
  },
  {
    "close": 37.34,
    "code": "BHP",
    "date": "2025-06-13",
    "high": 38.13,
    "id": 144782,
    "low": 37.28,
    "market": "ASX",
    "open": 37.96,
    "volume": 12726428
  },
  {
    "close": 37.44,
    "code": "BHP",
    "date": "2025-06-16",
    "high": 37.73,
    "id": 144783,
    "low": 36.92,
    "market": "ASX",
    "open": 36.94,
    "volume": 7995056
  },
  {
    "close": 37.3,
    "code": "BHP",
    "date": "2025-06-17",
    "high": 37.63,
    "id": 144784,
    "low": 36.98,
    "market": "ASX",
    "open": 37.5,
    "volume": 9427801
  },
  {
    "close": 36.86,
    "code": "BHP",
    "date": "2025-06-18",
    "high": 37,
    "id": 144785,
    "low": 36.69,
    "market": "ASX",
    "open": 36.84,
    "volume": 11797500
  },
  {
    "close": 36.13,
    "code": "BHP",
    "date": "2025-06-19",
    "high": 36.8,
    "id": 144786,
    "low": 36.02,
    "market": "ASX",
    "open": 36.71,
    "volume": 10816544
  },
  {
    "close": 36.21,
    "code": "BHP",
    "date": "2025-06-20",
    "high": 36.35,
    "id": 144787,
    "low": 35.88,
    "market": "ASX",
    "open": 36.33,
    "volume": 26948926
  },
  {
    "close": 35.64,
    "code": "BHP",
    "date": "2025-06-23",
    "high": 35.95,
    "id": 144788,
    "low": 35.52,
    "market": "ASX",
    "open": 35.8,
    "volume": 7060066
  },
  {
    "close": 36.48,
    "code": "BHP",
    "date": "2025-06-24",
    "high": 36.6,
    "id": 144789,
    "low": 36.1,
    "market": "ASX",
    "open": 36.45,
    "volume": 8837818
  },
  {
    "close": 36.11,
    "code": "BHP",
    "date": "2025-06-25",
    "high": 36.36,
    "id": 144790,
    "low": 35.79,
    "market": "ASX",
    "open": 36.13,
    "volume": 9462857
  },
  {
    "close": 36.12,
    "code": "BHP",
    "date": "2025-06-26",
    "high": 36.31,
    "id": 144791,
    "low": 35.98,
    "market": "ASX",
    "open": 36.04,
    "volume": 6796840
  },
  {
    "close": 37.53,
    "code": "BHP",
    "date": "2025-06-27",
    "high": 37.7,
    "id": 144792,
    "low": 36.8,
    "market": "ASX",
    "open": 36.87,
    "volume": 14244476
  },
  {
    "close": 36.75,
    "code": "BHP",
    "date": "2025-06-30",
    "high": 37.2,
    "id": 144793,
    "low": 36.54,
    "market": "ASX",
    "open": 37.18,
    "volume": 12990646
  },
  {
    "close": 36.57,
    "code": "BHP",
    "date": "2025-07-01",
    "high": 36.82,
    "id": 144794,
    "low": 36.36,
    "market": "ASX",
    "open": 36.64,
    "volume": 7856950
  },
  {
    "close": 37.2,
    "code": "BHP",
    "date": "2025-07-02",
    "high": 37.35,
    "id": 144795,
    "low": 36.76,
    "market": "ASX",
    "open": 37,
    "volume": 9145015
  },
  {
    "close": 39.27,
    "code": "BHP",
    "date": "2025-07-03",
    "high": 39.27,
    "id": 144796,
    "low": 38.37,
    "market": "ASX",
    "open": 38.4,
    "volume": 16531066
  },
  {
    "close": 38.73,
    "code": "BHP",
    "date": "2025-07-04",
    "high": 38.99,
    "id": 144797,
    "low": 38.45,
    "market": "ASX",
    "open": 38.6,
    "volume": 7905428
  },
  {
    "close": 38.6,
    "code": "BHP",
    "date": "2025-07-07",
    "high": 38.88,
    "id": 144798,
    "low": 38.53,
    "market": "ASX",
    "open": 38.65,
    "volume": 4267350
  },
  {
    "close": 38.24,
    "code": "BHP",
    "date": "2025-07-08",
    "high": 38.27,
    "id": 144799,
    "low": 37.56,
    "market": "ASX",
    "open": 37.77,
    "volume": 8029159
  },
  {
    "close": 37.85,
    "code": "BHP",
    "date": "2025-07-09",
    "high": 38.45,
    "id": 144800,
    "low": 37.85,
    "market": "ASX",
    "open": 38.3,
    "volume": 5880672
  },
  {
    "close": 38.3,
    "code": "BHP",
    "date": "2025-07-10",
    "high": 38.36,
    "id": 144801,
    "low": 37.74,
    "market": "ASX",
    "open": 37.88,
    "volume": 4923915
  },
  {
    "close": 39.36,
    "code": "BHP",
    "date": "2025-07-11",
    "high": 39.64,
    "id": 144802,
    "low": 39.22,
    "market": "ASX",
    "open": 39.27,
    "volume": 9460902
  },
  {
    "close": 39.73,
    "code": "BHP",
    "date": "2025-07-14",
    "high": 39.99,
    "id": 144803,
    "low": 39.51,
    "market": "ASX",
    "open": 39.59,
    "volume": 6582664
  },
  {
    "close": 39.39,
    "code": "BHP",
    "date": "2025-07-15",
    "high": 39.73,
    "id": 144804,
    "low": 39.22,
    "market": "ASX",
    "open": 39.62,
    "volume": 6061477
  },
  {
    "close": 39.11,
    "code": "BHP",
    "date": "2025-07-16",
    "high": 39.13,
    "id": 144805,
    "low": 38.66,
    "market": "ASX",
    "open": 38.77,
    "volume": 6479966
  },
  {
    "close": 39.11,
    "code": "BHP",
    "date": "2025-07-17",
    "high": 39.43,
    "id": 144806,
    "low": 39.06,
    "market": "ASX",
    "open": 39.13,
    "volume": 7380298
  },
  {
    "close": 40.29,
    "code": "BHP",
    "date": "2025-07-18",
    "high": 40.4,
    "id": 144807,
    "low": 39.67,
    "market": "ASX",
    "open": 39.82,
    "volume": 14757261
  },
  {
    "close": 40.46,
    "code": "BHP",
    "date": "2025-07-21",
    "high": 40.75,
    "id": 144808,
    "low": 39.93,
    "market": "ASX",
    "open": 40,
    "volume": 9694458
  },
  {
    "close": 41.51,
    "code": "BHP",
    "date": "2025-07-22",
    "high": 41.69,
    "id": 144809,
    "low": 40.77,
    "market": "ASX",
    "open": 40.89,
    "volume": 11430702
  },
  {
    "close": 41.85,
    "code": "BHP",
    "date": "2025-07-23",
    "high": 42.39,
    "id": 144810,
    "low": 41.81,
    "market": "ASX",
    "open": 41.86,
    "volume": 9875107
  },
  {
    "close": 41.6,
    "code": "BHP",
    "date": "2025-07-24",
    "high": 42.07,
    "id": 144811,
    "low": 41.46,
    "market": "ASX",
    "open": 41.99,
    "volume": 8832638
  },
  {
    "close": 40.8,
    "code": "BHP",
    "date": "2025-07-25",
    "high": 41.14,
    "id": 144812,
    "low": 40.66,
    "market": "ASX",
    "open": 40.86,
    "volume": 7481253
  },
  {
    "close": 40.3,
    "code": "BHP",
    "date": "2025-07-28",
    "high": 40.71,
    "id": 144813,
    "low": 40.2,
    "market": "ASX",
    "open": 40.57,
    "volume": 6193873
  },
  {
    "close": 40.42,
    "code": "BHP",
    "date": "2025-07-29",
    "high": 40.54,
    "id": 144814,
    "low": 40,
    "market": "ASX",
    "open": 40.15,
    "volume": 5510084
  },
  {
    "close": 40.22,
    "code": "BHP",
    "date": "2025-07-30",
    "high": 40.57,
    "id": 144815,
    "low": 40.2,
    "market": "ASX",
    "open": 40.25,
    "volume": 5662040
  },
  {
    "close": 39.25,
    "code": "BHP",
    "date": "2025-07-31",
    "high": 39.69,
    "id": 144816,
    "low": 39.18,
    "market": "ASX",
    "open": 39.27,
    "volume": 11994238
  },
  {
    "close": 39.22,
    "code": "BHP",
    "date": "2025-08-01",
    "high": 39.69,
    "id": 144817,
    "low": 39.22,
    "market": "ASX",
    "open": 39.45,
    "volume": 7455836
  },
  {
    "close": 39.59,
    "code": "BHP",
    "date": "2025-08-04",
    "high": 39.76,
    "id": 144818,
    "low": 39.18,
    "market": "ASX",
    "open": 39.38,
    "volume": 4916672
  },
  {
    "close": 39.8,
    "code": "BHP",
    "date": "2025-08-05",
    "high": 40.11,
    "id": 144819,
    "low": 39.74,
    "market": "ASX",
    "open": 39.81,
    "volume": 4548811
  },
  {
    "close": 39.9,
    "code": "BHP",
    "date": "2025-08-06",
    "high": 40.2,
    "id": 144820,
    "low": 39.7,
    "market": "ASX",
    "open": 39.79,
    "volume": 6271454
  },
  {
    "close": 39.87,
    "code": "BHP",
    "date": "2025-08-07",
    "high": 40.03,
    "id": 144821,
    "low": 39.68,
    "market": "ASX",
    "open": 39.98,
    "volume": 5398124
  },
  {
    "close": 40.21,
    "code": "BHP",
    "date": "2025-08-08",
    "high": 40.38,
    "id": 144822,
    "low": 39.9,
    "market": "ASX",
    "open": 40.07,
    "volume": 6130664
  },
  {
    "close": 40.87,
    "code": "BHP",
    "date": "2025-08-11",
    "high": 40.98,
    "id": 144823,
    "low": 40.65,
    "market": "ASX",
    "open": 40.7,
    "volume": 4867453
  },
  {
    "close": 41.26,
    "code": "BHP",
    "date": "2025-08-12",
    "high": 41.35,
    "id": 144824,
    "low": 40.82,
    "market": "ASX",
    "open": 40.99,
    "volume": 6893144
  },
  {
    "close": 41.73,
    "code": "BHP",
    "date": "2025-08-13",
    "high": 41.95,
    "id": 144825,
    "low": 41.41,
    "market": "ASX",
    "open": 41.42,
    "volume": 8188549
  },
  {
    "close": 41.51,
    "code": "BHP",
    "date": "2025-08-14",
    "high": 42.06,
    "id": 144826,
    "low": 41.27,
    "market": "ASX",
    "open": 41.85,
    "volume": 7815890
  },
  {
    "close": 41.96,
    "code": "BHP",
    "date": "2025-08-15",
    "high": 41.96,
    "id": 144827,
    "low": 41.39,
    "market": "ASX",
    "open": 41.55,
    "volume": 6932247
  },
  {
    "close": 41.47,
    "code": "BHP",
    "date": "2025-08-18",
    "high": 41.94,
    "id": 144828,
    "low": 41.45,
    "market": "ASX",
    "open": 41.77,
    "volume": 5271204
  },
  {
    "close": 42.12,
    "code": "BHP",
    "date": "2025-08-19",
    "high": 42.25,
    "id": 144829,
    "low": 41.34,
    "market": "ASX",
    "open": 41.6,
    "volume": 10003525
  },
  {
    "close": 41.75,
    "code": "BHP",
    "date": "2025-08-20",
    "high": 42.05,
    "id": 144830,
    "low": 40.9,
    "market": "ASX",
    "open": 41.39,
    "volume": 10296580
  },
  {
    "close": 42.06,
    "code": "BHP",
    "date": "2025-08-21",
    "high": 42.12,
    "id": 144831,
    "low": 41.68,
    "market": "ASX",
    "open": 41.9,
    "volume": 9435127
  },
  {
    "close": 42,
    "code": "BHP",
    "date": "2025-08-22",
    "high": 42.81,
    "id": 144832,
    "low": 42,
    "market": "ASX",
    "open": 42.66,
    "volume": 13494593
  },
  {
    "close": 43.14,
    "code": "BHP",
    "date": "2025-08-25",
    "high": 43.34,
    "id": 144833,
    "low": 42.82,
    "market": "ASX",
    "open": 42.9,
    "volume": 9440006
  },
  {
    "close": 42.65,
    "code": "BHP",
    "date": "2025-08-26",
    "high": 43.02,
    "id": 144834,
    "low": 42.59,
    "market": "ASX",
    "open": 43,
    "volume": 10597760
  },
  {
    "close": 43.22,
    "code": "BHP",
    "date": "2025-08-27",
    "high": 43.22,
    "id": 144835,
    "low": 42.59,
    "market": "ASX",
    "open": 42.7,
    "volume": 5993473
  },
  {
    "close": 43.01,
    "code": "BHP",
    "date": "2025-08-28",
    "high": 43.06,
    "id": 144836,
    "low": 42.76,
    "market": "ASX",
    "open": 42.91,
    "volume": 6259702
  },
  {
    "close": 43.19,
    "code": "BHP",
    "date": "2025-08-29",
    "high": 43.32,
    "id": 144837,
    "low": 42.8,
    "market": "ASX",
    "open": 43.1,
    "volume": 11355266
  },
  {
    "close": 42.7,
    "code": "BHP",
    "date": "2025-09-01",
    "high": 42.9,
    "id": 144838,
    "low": 42.48,
    "market": "ASX",
    "open": 42.7,
    "volume": 5679631
  },
  {
    "close": 42.85,
    "code": "BHP",
    "date": "2025-09-02",
    "high": 43.19,
    "id": 144839,
    "low": 42.6,
    "market": "ASX",
    "open": 42.6,
    "volume": 6959643
  },
  {
    "close": 42.29,
    "code": "BHP",
    "date": "2025-09-03",
    "high": 43.01,
    "id": 144840,
    "low": 42.25,
    "market": "ASX",
    "open": 42.7,
    "volume": 8236801
  },
  {
    "close": 41.98,
    "code": "BHP",
    "date": "2025-09-04",
    "high": 42.11,
    "id": 144841,
    "low": 41.66,
    "market": "ASX",
    "open": 41.75,
    "volume": 8318138
  },
  {
    "close": 41.61,
    "code": "BHP",
    "date": "2025-09-05",
    "high": 41.61,
    "id": 144842,
    "low": 41.33,
    "market": "ASX",
    "open": 41.4,
    "volume": 8268770
  },
  {
    "close": 41.37,
    "code": "BHP",
    "date": "2025-09-08",
    "high": 41.79,
    "id": 144843,
    "low": 41.31,
    "market": "ASX",
    "open": 41.63,
    "volume": 7131110
  },
  {
    "close": 40.97,
    "code": "BHP",
    "date": "2025-09-09",
    "high": 41.18,
    "id": 144844,
    "low": 40.7,
    "market": "ASX",
    "open": 41.13,
    "volume": 11339058
  },
  {
    "close": 40.46,
    "code": "BHP",
    "date": "2025-09-10",
    "high": 40.56,
    "id": 144845,
    "low": 40.18,
    "market": "ASX",
    "open": 40.32,
    "volume": 7263768
  },
  {
    "close": 40.27,
    "code": "BHP",
    "date": "2025-09-11",
    "high": 40.48,
    "id": 144846,
    "low": 40.15,
    "market": "ASX",
    "open": 40.3,
    "volume": 5904258
  },
  {
    "close": 40.81,
    "code": "BHP",
    "date": "2025-09-12",
    "high": 40.89,
    "id": 144847,
    "low": 40.46,
    "market": "ASX",
    "open": 40.62,
    "volume": 5093254
  },
  {
    "close": 40.58,
    "code": "BHP",
    "date": "2025-09-15",
    "high": 40.85,
    "id": 144848,
    "low": 40.38,
    "market": "ASX",
    "open": 40.75,
    "volume": 4485886
  },
  {
    "close": 40.77,
    "code": "BHP",
    "date": "2025-09-16",
    "high": 41.14,
    "id": 144849,
    "low": 40.77,
    "market": "ASX",
    "open": 40.92,
    "volume": 7464437
  },
  {
    "close": 40.31,
    "code": "BHP",
    "date": "2025-09-17",
    "high": 40.62,
    "id": 144850,
    "low": 40.14,
    "market": "ASX",
    "open": 40.42,
    "volume": 7379291
  },
  {
    "close": 39.97,
    "code": "BHP",
    "date": "2025-09-18",
    "high": 40.19,
    "id": 144851,
    "low": 39.88,
    "market": "ASX",
    "open": 40,
    "volume": 7823281
  },
  {
    "close": 39.64,
    "code": "BHP",
    "date": "2025-09-19",
    "high": 40.2,
    "id": 144852,
    "low": 39.3,
    "market": "ASX",
    "open": 39.77,
    "volume": 22847291
  },
  {
    "close": 40.03,
    "code": "BHP",
    "date": "2025-09-22",
    "high": 40.34,
    "id": 144853,
    "low": 39.57,
    "market": "ASX",
    "open": 39.65,
    "volume": 7393512
  },
  {
    "close": 40.22,
    "code": "BHP",
    "date": "2025-09-23",
    "high": 40.69,
    "id": 144854,
    "low": 40.08,
    "market": "ASX",
    "open": 40.3,
    "volume": 6219232
  },
  {
    "close": 40.24,
    "code": "BHP",
    "date": "2025-09-24",
    "high": 40.47,
    "id": 144855,
    "low": 40,
    "market": "ASX",
    "open": 40.45,
    "volume": 7179771
  },
  {
    "close": 41.67,
    "code": "BHP",
    "date": "2025-09-25",
    "high": 41.89,
    "id": 144856,
    "low": 40.9,
    "market": "ASX",
    "open": 40.99,
    "volume": 12092384
  },
  {
    "close": 42.22,
    "code": "BHP",
    "date": "2025-09-26",
    "high": 41.89,
    "id": 144857,
    "low": 42.08,
    "market": "ASX",
    "open": 40.99,
    "volume": 10922787
  },
  {
    "close": 41.91,
    "code": "BHP",
    "date": "2025-09-29",
    "high": 41.99,
    "id": 144858,
    "low": 41.51,
    "market": "ASX",
    "open": 41.8,
    "volume": 9744918
  },
  {
    "close": 42.53,
    "code": "BHP",
    "date": "2025-09-30",
    "high": 42.94,
    "id": 144859,
    "low": 42.53,
    "market": "ASX",
    "open": 42.75,
    "volume": 12591714
  },
  {
    "close": 41.47,
    "code": "BHP",
    "date": "2025-10-01",
    "high": 42.12,
    "id": 144860,
    "low": 41.42,
    "market": "ASX",
    "open": 42.05,
    "volume": 11041231
  },
  {
    "close": 41.94,
    "code": "BHP",
    "date": "2025-10-02",
    "high": 42.34,
    "id": 144861,
    "low": 41.84,
    "market": "ASX",
    "open": 41.9,
    "volume": 7075579
  },
  {
    "close": 42.08,
    "code": "BHP",
    "date": "2025-10-03",
    "high": 42.2,
    "id": 144862,
    "low": 41.8,
    "market": "ASX",
    "open": 41.9,
    "volume": 4996434
  },
  {
    "close": 41.9,
    "code": "BHP",
    "date": "2025-10-06",
    "high": 42.16,
    "id": 144863,
    "low": 41.8,
    "market": "ASX",
    "open": 42,
    "volume": 5399935
  },
  {
    "close": 41.96,
    "code": "BHP",
    "date": "2025-10-07",
    "high": 42.32,
    "id": 144864,
    "low": 41.87,
    "market": "ASX",
    "open": 41.9,
    "volume": 6151818
  },
  {
    "close": 41.89,
    "code": "BHP",
    "date": "2025-10-08",
    "high": 42.12,
    "id": 144865,
    "low": 41.82,
    "market": "ASX",
    "open": 41.95,
    "volume": 6737624
  },
  {
    "close": 43.11,
    "code": "BHP",
    "date": "2025-10-09",
    "high": 43.11,
    "id": 144866,
    "low": 42.5,
    "market": "ASX",
    "open": 42.61,
    "volume": 14134217
  },
  {
    "close": 42.22,
    "code": "BHP",
    "date": "2025-10-10",
    "high": 42.9,
    "id": 144867,
    "low": 42.16,
    "market": "ASX",
    "open": 42.88,
    "volume": 10040660
  },
  {
    "close": 41.89,
    "code": "BHP",
    "date": "2025-10-13",
    "high": 42.07,
    "id": 144868,
    "low": 41.52,
    "market": "ASX",
    "open": 41.86,
    "volume": 8378334
  },
  {
    "close": 42.79,
    "code": "BHP",
    "date": "2025-10-14",
    "high": 43.12,
    "id": 144869,
    "low": 42.43,
    "market": "ASX",
    "open": 42.56,
    "volume": 9509568
  },
  {
    "close": 43.54,
    "code": "BHP",
    "date": "2025-10-15",
    "high": 43.54,
    "id": 144870,
    "low": 42.93,
    "market": "ASX",
    "open": 43.23,
    "volume": 9777081
  },
  {
    "close": 43.77,
    "code": "BHP",
    "date": "2025-10-16",
    "high": 43.84,
    "id": 144871,
    "low": 43.34,
    "market": "ASX",
    "open": 43.48,
    "volume": 10243494
  },
  {
    "close": 43.6,
    "code": "BHP",
    "date": "2025-10-17",
    "high": 43.64,
    "id": 144872,
    "low": 43.21,
    "market": "ASX",
    "open": 43.56,
    "volume": 10756381
  },
  {
    "close": 43.14,
    "code": "BHP",
    "date": "2025-10-20",
    "high": 43.14,
    "id": 144873,
    "low": 42.79,
    "market": "ASX",
    "open": 42.85,
    "volume": 8239848
  },
  {
    "close": 44.13,
    "code": "BHP",
    "date": "2025-10-21",
    "high": 44.33,
    "id": 144874,
    "low": 43.73,
    "market": "ASX",
    "open": 43.8,
    "volume": 9494518
  },
  {
    "close": 43.51,
    "code": "BHP",
    "date": "2025-10-22",
    "high": 43.68,
    "id": 144875,
    "low": 43.11,
    "market": "ASX",
    "open": 43.4,
    "volume": 7313867
  },
  {
    "close": 43.01,
    "code": "BHP",
    "date": "2025-10-23",
    "high": 43.01,
    "id": 144876,
    "low": 42.29,
    "market": "ASX",
    "open": 42.75,
    "volume": 7054913
  },
  {
    "close": 43.24,
    "code": "BHP",
    "date": "2025-10-24",
    "high": 43.24,
    "id": 144877,
    "low": 42.67,
    "market": "ASX",
    "open": 42.75,
    "volume": 7404860
  },
  {
    "close": 43.54,
    "code": "BHP",
    "date": "2025-10-27",
    "high": 43.63,
    "id": 144878,
    "low": 42.95,
    "market": "ASX",
    "open": 43.05,
    "volume": 6414129
  },
  {
    "close": 43.34,
    "code": "BHP",
    "date": "2025-10-28",
    "high": 43.68,
    "id": 144879,
    "low": 42.95,
    "market": "ASX",
    "open": 43.35,
    "volume": 8925277
  },
  {
    "close": 43.89,
    "code": "BHP",
    "date": "2025-10-29",
    "high": 43.93,
    "id": 144880,
    "low": 43.35,
    "market": "ASX",
    "open": 43.42,
    "volume": 6122873
  },
  {
    "close": 43.88,
    "code": "BHP",
    "date": "2025-10-30",
    "high": 44.55,
    "id": 144881,
    "low": 43.81,
    "market": "ASX",
    "open": 44.28,
    "volume": 7203994
  },
  {
    "close": 43.45,
    "code": "BHP",
    "date": "2025-10-31",
    "high": 44.09,
    "id": 144882,
    "low": 43.45,
    "market": "ASX",
    "open": 43.6,
    "volume": 7562306
  },
  {
    "close": 43.37,
    "code": "BHP",
    "date": "2025-11-03",
    "high": 43.65,
    "id": 144883,
    "low": 43.07,
    "market": "ASX",
    "open": 43.45,
    "volume": 5473353
  },
  {
    "close": 42.54,
    "code": "BHP",
    "date": "2025-11-04",
    "high": 43.13,
    "id": 144884,
    "low": 42.51,
    "market": "ASX",
    "open": 43,
    "volume": 6834944
  },
  {
    "close": 42.34,
    "code": "BHP",
    "date": "2025-11-05",
    "high": 42.45,
    "id": 144885,
    "low": 41.91,
    "market": "ASX",
    "open": 42.35,
    "volume": 7637826
  },
  {
    "close": 43,
    "code": "BHP",
    "date": "2025-11-06",
    "high": 43.33,
    "id": 144886,
    "low": 42.66,
    "market": "ASX",
    "open": 42.86,
    "volume": 6475644
  },
  {
    "close": 42.65,
    "code": "BHP",
    "date": "2025-11-07",
    "high": 42.94,
    "id": 144887,
    "low": 42.43,
    "market": "ASX",
    "open": 42.88,
    "volume": 6125961
  },
  {
    "close": 42.65,
    "code": "BHP",
    "date": "2025-11-10",
    "high": 42.8,
    "id": 144888,
    "low": 42.47,
    "market": "ASX",
    "open": 42.59,
    "volume": 5094784
  },
  {
    "close": 42.79,
    "code": "BHP",
    "date": "2025-11-11",
    "high": 43,
    "id": 144889,
    "low": 42.57,
    "market": "ASX",
    "open": 43,
    "volume": 5884672
  },
  {
    "close": 43.06,
    "code": "BHP",
    "date": "2025-11-12",
    "high": 43.29,
    "id": 144890,
    "low": 42.77,
    "market": "ASX",
    "open": 42.85,
    "volume": 5169069
  },
  {
    "close": 43.33,
    "code": "BHP",
    "date": "2025-11-13",
    "high": 43.45,
    "id": 144891,
    "low": 42.83,
    "market": "ASX",
    "open": 43.33,
    "volume": 7686716
  },
  {
    "close": 42.75,
    "code": "BHP",
    "date": "2025-11-14",
    "high": 42.9,
    "id": 144892,
    "low": 42.37,
    "market": "ASX",
    "open": 42.77,
    "volume": 6262461
  },
  {
    "close": 42.48,
    "code": "BHP",
    "date": "2025-11-17",
    "high": 42.56,
    "id": 144893,
    "low": 42.01,
    "market": "ASX",
    "open": 42.13,
    "volume": 5526619
  },
  {
    "close": 40.9,
    "code": "BHP",
    "date": "2025-11-18",
    "high": 42.11,
    "id": 144894,
    "low": 40.8,
    "market": "ASX",
    "open": 42,
    "volume": 12252545
  },
  {
    "close": 40.95,
    "code": "BHP",
    "date": "2025-11-19",
    "high": 41.46,
    "id": 144895,
    "low": 40.95,
    "market": "ASX",
    "open": 41.4,
    "volume": 7407413
  },
  {
    "close": 41.72,
    "code": "BHP",
    "date": "2025-11-20",
    "high": 41.8,
    "id": 144896,
    "low": 41.03,
    "market": "ASX",
    "open": 41.1,
    "volume": 8072596
  },
  {
    "close": 40.37,
    "code": "BHP",
    "date": "2025-11-21",
    "high": 40.75,
    "id": 144897,
    "low": 40.21,
    "market": "ASX",
    "open": 40.5,
    "volume": 10232751
  },
  {
    "close": 40.62,
    "code": "BHP",
    "date": "2025-11-24",
    "high": 40.89,
    "id": 144898,
    "low": 40.17,
    "market": "ASX",
    "open": 40.78,
    "volume": 14778037
  },
  {
    "close": 41.01,
    "code": "BHP",
    "date": "2025-11-25",
    "high": 41.03,
    "id": 144899,
    "low": 40.62,
    "market": "ASX",
    "open": 40.93,
    "volume": 8020389
  },
  {
    "close": 41.82,
    "code": "BHP",
    "date": "2025-11-26",
    "high": 41.84,
    "id": 144900,
    "low": 41.49,
    "market": "ASX",
    "open": 41.62,
    "volume": 7359870
  },
  {
    "close": 41.74,
    "code": "BHP",
    "date": "2025-11-27",
    "high": 41.95,
    "id": 144901,
    "low": 41.56,
    "market": "ASX",
    "open": 41.73,
    "volume": 4450872
  },
  {
    "close": 41.67,
    "code": "BHP",
    "date": "2025-11-28",
    "high": 41.76,
    "id": 144902,
    "low": 41.29,
    "market": "ASX",
    "open": 41.35,
    "volume": 8041166
  },
  {
    "close": 42.08,
    "code": "BHP",
    "date": "2025-12-01",
    "high": 42.3,
    "id": 144903,
    "low": 41.81,
    "market": "ASX",
    "open": 41.99,
    "volume": 6152730
  },
  {
    "close": 42.56,
    "code": "BHP",
    "date": "2025-12-02",
    "high": 42.9,
    "id": 144904,
    "low": 42.39,
    "market": "ASX",
    "open": 42.9,
    "volume": 7321330
  },
  {
    "close": 42.96,
    "code": "BHP",
    "date": "2025-12-03",
    "high": 43.04,
    "id": 144905,
    "low": 42.56,
    "market": "ASX",
    "open": 42.84,
    "volume": 7357811
  },
  {
    "close": 44.5,
    "code": "BHP",
    "date": "2025-12-04",
    "high": 44.6,
    "id": 144906,
    "low": 43.75,
    "market": "ASX",
    "open": 43.85,
    "volume": 11656068
  },
  {
    "close": 44.84,
    "code": "BHP",
    "date": "2025-12-05",
    "high": 44.84,
    "id": 144907,
    "low": 44.36,
    "market": "ASX",
    "open": 44.55,
    "volume": 9843381
  },
  {
    "close": 44.47,
    "code": "BHP",
    "date": "2025-12-08",
    "high": 44.84,
    "id": 144908,
    "low": 44.35,
    "market": "ASX",
    "open": 44.82,
    "volume": 5324347
  },
  {
    "close": 44.3,
    "code": "BHP",
    "date": "2025-12-09",
    "high": 44.79,
    "id": 144909,
    "low": 44.1,
    "market": "ASX",
    "open": 44.15,
    "volume": 7148967
  },
  {
    "close": 44.54,
    "code": "BHP",
    "date": "2025-12-10",
    "high": 44.81,
    "id": 144910,
    "low": 44.13,
    "market": "ASX",
    "open": 44.39,
    "volume": 6608310
  },
  {
    "close": 45.1,
    "code": "BHP",
    "date": "2025-12-11",
    "high": 45.49,
    "id": 144911,
    "low": 44.8,
    "market": "ASX",
    "open": 45.31,
    "volume": 7930105
  },
  {
    "close": 45.59,
    "code": "BHP",
    "date": "2025-12-12",
    "high": 45.98,
    "id": 144912,
    "low": 45.45,
    "market": "ASX",
    "open": 45.68,
    "volume": 7547616
  },
  {
    "close": 44.27,
    "code": "BHP",
    "date": "2025-12-15",
    "high": 45.14,
    "id": 144913,
    "low": 44.06,
    "market": "ASX",
    "open": 45.01,
    "volume": 8496021
  },
  {
    "close": 44.24,
    "code": "BHP",
    "date": "2025-12-16",
    "high": 44.6,
    "id": 144914,
    "low": 44.06,
    "market": "ASX",
    "open": 44.3,
    "volume": 9553809
  },
  {
    "close": 44.41,
    "code": "BHP",
    "date": "2025-12-17",
    "high": 44.45,
    "id": 144915,
    "low": 43.96,
    "market": "ASX",
    "open": 44.16,
    "volume": 7866827
  },
  {
    "close": 44.88,
    "code": "BHP",
    "date": "2025-12-18",
    "high": 44.88,
    "id": 144916,
    "low": 44.38,
    "market": "ASX",
    "open": 44.4,
    "volume": 9640423
  },
  {
    "close": 44.36,
    "code": "BHP",
    "date": "2025-12-19",
    "high": 44.84,
    "id": 144917,
    "low": 44.13,
    "market": "ASX",
    "open": 44.55,
    "volume": 23705950
  },
  {
    "close": 45.07,
    "code": "BHP",
    "date": "2025-12-22",
    "high": 45.17,
    "id": 144918,
    "low": 44.72,
    "market": "ASX",
    "open": 44.85,
    "volume": 5527196
  },
  {
    "close": 45.58,
    "code": "BHP",
    "date": "2025-12-23",
    "high": 45.71,
    "id": 144919,
    "low": 45.3,
    "market": "ASX",
    "open": 45.6,
    "volume": 4534942
  },
  {
    "close": 45.62,
    "code": "BHP",
    "date": "2025-12-24",
    "high": 45.78,
    "id": 144920,
    "low": 45.5,
    "market": "ASX",
    "open": 45.78,
    "volume": 2112731
  },
  {
    "close": 45.45,
    "code": "BHP",
    "date": "2025-12-29",
    "high": 46.03,
    "id": 144921,
    "low": 45.45,
    "market": "ASX",
    "open": 45.9,
    "volume": 4680346
  },
  {
    "close": 45.1,
    "code": "BHP",
    "date": "2025-12-30",
    "high": 45.39,
    "id": 144922,
    "low": 44.7,
    "market": "ASX",
    "open": 44.8,
    "volume": 4171339
  },
  {
    "close": 45.49,
    "code": "BHP",
    "date": "2025-12-31",
    "high": 45.62,
    "id": 144923,
    "low": 45.17,
    "market": "ASX",
    "open": 45.5,
    "volume": 3951995
  },
  {
    "close": 45.76,
    "code": "BHP",
    "date": "2026-01-02",
    "high": 45.76,
    "id": 144924,
    "low": 45.37,
    "market": "ASX",
    "open": 45.53,
    "volume": 3035952
  },
  {
    "close": 46.48,
    "code": "BHP",
    "date": "2026-01-05",
    "high": 46.65,
    "id": 144925,
    "low": 45.91,
    "market": "ASX",
    "open": 46,
    "volume": 5888054
  },
  {
    "close": 47.22,
    "code": "BHP",
    "date": "2026-01-06",
    "high": 47.37,
    "id": 144926,
    "low": 46.82,
    "market": "ASX",
    "open": 47.32,
    "volume": 8048129
  },
  {
    "close": 47.7,
    "code": "BHP",
    "date": "2026-01-07",
    "high": 48.49,
    "id": 144927,
    "low": 47.41,
    "market": "ASX",
    "open": 48.08,
    "volume": 7268819
  },
  {
    "close": 47.34,
    "code": "BHP",
    "date": "2026-01-08",
    "high": 47.78,
    "id": 144928,
    "low": 47.17,
    "market": "ASX",
    "open": 47.58,
    "volume": 6359264
  },
  {
    "close": 47.72,
    "code": "BHP",
    "date": "2026-01-09",
    "high": 47.95,
    "id": 144929,
    "low": 46.68,
    "market": "ASX",
    "open": 46.9,
    "volume": 10551269
  },
  {
    "close": 46.51,
    "code": "BHP",
    "date": "2026-01-12",
    "high": 46.9,
    "id": 144930,
    "low": 46.15,
    "market": "ASX",
    "open": 46.55,
    "volume": 11129168
  },
  {
    "close": 47.58,
    "code": "BHP",
    "date": "2026-01-13",
    "high": 47.86,
    "id": 144931,
    "low": 46.99,
    "market": "ASX",
    "open": 47.18,
    "volume": 8959090
  },
  {
    "close": 48.12,
    "code": "BHP",
    "date": "2026-01-14",
    "high": 48.25,
    "id": 144932,
    "low": 47.84,
    "market": "ASX",
    "open": 48.24,
    "volume": 7244698
  },
  {
    "close": 49.37,
    "code": "BHP",
    "date": "2026-01-15",
    "high": 49.75,
    "id": 144933,
    "low": 49,
    "market": "ASX",
    "open": 49.14,
    "volume": 14145071
  },
  {
    "close": 48.99,
    "code": "BHP",
    "date": "2026-01-16",
    "high": 49.2,
    "id": 144934,
    "low": 48.73,
    "market": "ASX",
    "open": 49.13,
    "volume": 12369669
  },
  {
    "close": 48.75,
    "code": "BHP",
    "date": "2026-01-19",
    "high": 49.1,
    "id": 144935,
    "low": 48.34,
    "market": "ASX",
    "open": 48.71,
    "volume": 4876453
  },
  {
    "close": 47.78,
    "code": "BHP",
    "date": "2026-01-20",
    "high": 49.06,
    "id": 144936,
    "low": 47.74,
    "market": "ASX",
    "open": 49,
    "volume": 6618315
  },
  {
    "close": 48.48,
    "code": "BHP",
    "date": "2026-01-21",
    "high": 48.5,
    "id": 144937,
    "low": 47.25,
    "market": "ASX",
    "open": 47.43,
    "volume": 6236093
  },
  {
    "close": 48.08,
    "code": "BHP",
    "date": "2026-01-22",
    "high": 49.36,
    "id": 144938,
    "low": 47.92,
    "market": "ASX",
    "open": 49.29,
    "volume": 8157995
  },
  {
    "close": 48.43,
    "code": "BHP",
    "date": "2026-01-23",
    "high": 48.5,
    "id": 144939,
    "low": 48.03,
    "market": "ASX",
    "open": 48.5,
    "volume": 10394402
  },
  {
    "close": 49.75,
    "code": "BHP",
    "date": "2026-01-27",
    "high": 50.08,
    "id": 144940,
    "low": 49.55,
    "market": "ASX",
    "open": 49.75,
    "volume": 11555637
  },
  {
    "close": 50.6,
    "code": "BHP",
    "date": "2026-01-28",
    "high": 50.86,
    "id": 144941,
    "low": 49.88,
    "market": "ASX",
    "open": 50.5,
    "volume": 10136377
  },
  {
    "close": 51.51,
    "code": "BHP",
    "date": "2026-01-29",
    "high": 51.68,
    "id": 144942,
    "low": 49.92,
    "market": "ASX",
    "open": 50.25,
    "volume": 14752411
  },
  {
    "close": 50.57,
    "code": "BHP",
    "date": "2026-01-30",
    "high": 52.09,
    "id": 144943,
    "low": 50.12,
    "market": "ASX",
    "open": 51.84,
    "volume": 17998156
  },
  {
    "close": 49.42,
    "code": "BHP",
    "date": "2026-02-02",
    "high": 49.89,
    "id": 144944,
    "low": 48.75,
    "market": "ASX",
    "open": 49.4,
    "volume": 12250669
  },
  {
    "close": 50.13,
    "code": "BHP",
    "date": "2026-02-03",
    "high": 50.59,
    "id": 144945,
    "low": 49.7,
    "market": "ASX",
    "open": 49.9,
    "volume": 9627914
  },
  {
    "close": 52.4,
    "code": "BHP",
    "date": "2026-02-04",
    "high": 52.54,
    "id": 144946,
    "low": 51.58,
    "market": "ASX",
    "open": 51.77,
    "volume": 10349118
  },
  {
    "close": 50.36,
    "code": "BHP",
    "date": "2026-02-05",
    "high": 51.87,
    "id": 144947,
    "low": 50.32,
    "market": "ASX",
    "open": 51.6,
    "volume": 8874522
  },
  {
    "close": 48.79,
    "code": "BHP",
    "date": "2026-02-06",
    "high": 49.83,
    "id": 144948,
    "low": 48.5,
    "market": "ASX",
    "open": 49.83,
    "volume": 11533731
  },
  {
    "close": 49.73,
    "code": "BHP",
    "date": "2026-02-09",
    "high": 49.98,
    "id": 144949,
    "low": 49.61,
    "market": "ASX",
    "open": 49.9,
    "volume": 7529009
  },
  {
    "close": 50.26,
    "code": "BHP",
    "date": "2026-02-10",
    "high": 50.7,
    "id": 144950,
    "low": 50.1,
    "market": "ASX",
    "open": 50.7,
    "volume": 7423941
  },
  {
    "close": 51.07,
    "code": "BHP",
    "date": "2026-02-11",
    "high": 51.17,
    "id": 144951,
    "low": 50.46,
    "market": "ASX",
    "open": 50.98,
    "volume": 6146113
  },
  {
    "close": 52.09,
    "code": "BHP",
    "date": "2026-02-12",
    "high": 52.64,
    "id": 144952,
    "low": 51.95,
    "market": "ASX",
    "open": 52.22,
    "volume": 8872940
  },
  {
    "close": 51.13,
    "code": "BHP",
    "date": "2026-02-13",
    "high": 51.71,
    "id": 144953,
    "low": 51.11,
    "market": "ASX",
    "open": 51.4,
    "volume": 11676856
  },
  {
    "close": 50.36,
    "code": "BHP",
    "date": "2026-02-16",
    "high": 51.67,
    "id": 144954,
    "low": 49.83,
    "market": "ASX",
    "open": 51.55,
    "volume": 8634571
  },
  {
    "close": 52.74,
    "code": "BHP",
    "date": "2026-02-17",
    "high": 54.2,
    "id": 144955,
    "low": 52.72,
    "market": "ASX",
    "open": 53,
    "volume": 13615340
  },
  {
    "close": 52.29,
    "code": "BHP",
    "date": "2026-02-18",
    "high": 52.46,
    "id": 561376,
    "low": 51.55,
    "market": "ASX",
    "open": 51.65,
    "volume": 8209195
  },
  {
    "close": 53.23,
    "code": "BHP",
    "date": "2026-02-19",
    "high": 53.82,
    "id": 562039,
    "low": 52.82,
    "market": "ASX",
    "open": 53.3,
    "volume": 12660242
  },
  {
    "close": 53.33,
    "code": "BHP",
    "date": "2026-02-20",
    "high": 53.93,
    "id": 562735,
    "low": 52.93,
    "market": "ASX",
    "open": 53,
    "volume": 14168676
  },
  {
    "close": 54.02,
    "code": "BHP",
    "date": "2026-02-23",
    "high": 54.75,
    "id": 563620,
    "low": 53.74,
    "market": "ASX",
    "open": 54.5,
    "volume": 8586992
  },
  {
    "close": 54.75,
    "code": "BHP",
    "date": "2026-02-24",
    "high": 55.33,
    "id": 564819,
    "low": 54.4,
    "market": "ASX",
    "open": 55.2,
    "volume": 9869664
  },
  {
    "close": 56.51,
    "code": "BHP",
    "date": "2026-02-25",
    "high": 56.51,
    "id": 566132,
    "low": 55.56,
    "market": "ASX",
    "open": 56.15,
    "volume": 13222274
  },
  {
    "close": 57.75,
    "code": "BHP",
    "date": "2026-02-26",
    "high": 58.29,
    "id": 566133,
    "low": 57.23,
    "market": "ASX",
    "open": 57.34,
    "volume": 14358999
  },
  {
    "close": 58.41,
    "code": "BHP",
    "date": "2026-02-27",
    "high": 58.41,
    "id": 566496,
    "low": 56.88,
    "market": "ASX",
    "open": 56.91,
    "volume": 23997982
  },
  {
    "close": 59.25,
    "code": "BHP",
    "date": "2026-03-02",
    "high": 59.25,
    "id": 567756,
    "low": 57.08,
    "market": "ASX",
    "open": 57.1,
    "volume": 10362367
  },
  {
    "close": 57.7,
    "code": "BHP",
    "date": "2026-03-03",
    "high": 59.39,
    "id": 568429,
    "low": 57.53,
    "market": "ASX",
    "open": 59.25,
    "volume": 15181122
  },
  {
    "close": 55.68,
    "code": "BHP",
    "date": "2026-03-04",
    "high": 56,
    "id": 569189,
    "low": 55.11,
    "market": "ASX",
    "open": 55.51,
    "volume": 15429993
  },
  {
    "close": 55.15,
    "code": "BHP",
    "date": "2026-03-05",
    "high": 55.42,
    "id": 569703,
    "low": 54.55,
    "market": "ASX",
    "open": 54.6,
    "volume": 16282660
  },
  {
    "close": 52.81,
    "code": "BHP",
    "date": "2026-03-06",
    "high": 53.11,
    "id": 570742,
    "low": 51.85,
    "market": "ASX",
    "open": 53.02,
    "volume": 17135396
  },
  {
    "close": 50.1,
    "code": "BHP",
    "date": "2026-03-09",
    "high": 50.69,
    "id": 571756,
    "low": 49.27,
    "market": "ASX",
    "open": 50.16,
    "volume": 16801718
  },
  {
    "close": 51.23,
    "code": "BHP",
    "date": "2026-03-10",
    "high": 51.68,
    "id": 572505,
    "low": 50.97,
    "market": "ASX",
    "open": 51.38,
    "volume": 10571912
  },
  {
    "close": 51.96,
    "code": "BHP",
    "date": "2026-03-11",
    "high": 52.14,
    "id": 572999,
    "low": 51.56,
    "market": "ASX",
    "open": 51.6,
    "volume": 8773957
  },
  {
    "close": 50.98,
    "code": "BHP",
    "date": "2026-03-12",
    "high": 51.38,
    "id": 573697,
    "low": 50.71,
    "market": "ASX",
    "open": 50.75,
    "volume": 8899505
  },
  {
    "close": 49.8,
    "code": "BHP",
    "date": "2026-03-13",
    "high": 50.4,
    "id": 573698,
    "low": 49.64,
    "market": "ASX",
    "open": 49.8,
    "volume": 11959929
  },
  {
    "close": 49.19,
    "code": "BHP",
    "date": "2026-03-16",
    "high": 49.79,
    "id": 575315,
    "low": 48.88,
    "market": "ASX",
    "open": 49.5,
    "volume": 7910037
  },
  {
    "close": 49.73,
    "code": "BHP",
    "date": "2026-03-17",
    "high": 50.07,
    "id": 576576,
    "low": 49.31,
    "market": "ASX",
    "open": 50.07,
    "volume": 7164541
  },
  {
    "close": 50.09,
    "code": "BHP",
    "date": "2026-03-18",
    "high": 50.32,
    "id": 576577,
    "low": 49.74,
    "market": "ASX",
    "open": 49.94,
    "volume": 7110651
  },
  {
    "close": 48.35,
    "code": "BHP",
    "date": "2026-03-19",
    "high": 48.76,
    "id": 577620,
    "low": 48.21,
    "market": "ASX",
    "open": 48.66,
    "volume": 12248012
  },
  {
    "close": 47.47,
    "code": "BHP",
    "date": "2026-03-20",
    "high": 47.77,
    "id": 578165,
    "low": 46.59,
    "market": "ASX",
    "open": 47.19,
    "volume": 34279073
  },
  {
    "close": 47.11,
    "code": "BHP",
    "date": "2026-03-23",
    "high": 47.11,
    "id": 579381,
    "low": 46.06,
    "market": "ASX",
    "open": 46.32,
    "volume": 10896675
  },
  {
    "close": 48.52,
    "code": "BHP",
    "date": "2026-03-24",
    "high": 49.06,
    "id": 579909,
    "low": 48.12,
    "market": "ASX",
    "open": 49,
    "volume": 10432952
  },
  {
    "close": 50.12,
    "code": "BHP",
    "date": "2026-03-25",
    "high": 50.38,
    "id": 580905,
    "low": 49.56,
    "market": "ASX",
    "open": 49.9,
    "volume": 8946111
  },
  {
    "close": 50.23,
    "code": "BHP",
    "date": "2026-03-26",
    "high": 50.6,
    "id": 582094,
    "low": 49.73,
    "market": "ASX",
    "open": 50.58,
    "volume": 11257708
  },
  {
    "close": 50.37,
    "code": "BHP",
    "date": "2026-03-27",
    "high": 50.37,
    "id": 582095,
    "low": 49.72,
    "market": "ASX",
    "open": 50,
    "volume": 9964987
  },
  {
    "close": 50.43,
    "code": "BHP",
    "date": "2026-03-30",
    "high": 50.77,
    "id": 583451,
    "low": 50.06,
    "market": "ASX",
    "open": 50.35,
    "volume": 8245917
  },
  {
    "close": 50.39,
    "code": "BHP",
    "date": "2026-03-31",
    "high": 50.78,
    "id": 583712,
    "low": 49.76,
    "market": "ASX",
    "open": 50.5,
    "volume": 11574797
  },
  {
    "close": 52.56,
    "code": "BHP",
    "date": "2026-04-01",
    "high": 53.09,
    "id": 585088,
    "low": 52.35,
    "market": "ASX",
    "open": 52.5,
    "volume": 9362507
  },
  {
    "close": 51.23,
    "code": "BHP",
    "date": "2026-04-02",
    "high": 53.07,
    "id": 585089,
    "low": 51.02,
    "market": "ASX",
    "open": 52.91,
    "volume": 8654569
  },
  {
    "close": 52.92,
    "code": "BHP",
    "date": "2026-04-07",
    "high": 53.02,
    "id": 586492,
    "low": 52.34,
    "market": "ASX",
    "open": 52.85,
    "volume": 6945522
  },
  {
    "close": 54.53,
    "code": "BHP",
    "date": "2026-04-08",
    "high": 55.8,
    "id": 586493,
    "low": 54.5,
    "market": "ASX",
    "open": 55.8,
    "volume": 9231453
  },
  {
    "close": 54.56,
    "code": "BHP",
    "date": "2026-04-09",
    "high": 54.56,
    "id": 588061,
    "low": 53.76,
    "market": "ASX",
    "open": 53.98,
    "volume": 6828429
  },
  {
    "close": 53.98,
    "code": "BHP",
    "date": "2026-04-10",
    "high": 54.56,
    "id": 588314,
    "low": 53.6,
    "market": "ASX",
    "open": 54.56,
    "volume": 7381772
  }
]            """;
}
