package almacen.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Utilidad encargada de interpretar planillas de carga de stock en formato
 * Microsoft Excel nativo (.xlsx, estándar OpenXML) y valores separados por comas (.csv).
 *
 * Diseño de 3er Año: Implementa la lectura de archivos .xlsx descomprimiendo en memoria
 * el contenedor ZIP OpenXML y procesando de forma imperativa la tabla de textos compartidos
 * (xl/sharedStrings.xml) y las celdas de la hoja (xl/worksheets/sheet1.xml) mediante el
 * parser DOM estándar de Java (javax.xml.parsers), garantizando total portabilidad
 * sin depender de librerías externas pesadas.
 */
public class LectorPlanillaExcel {

    /**
     * Interpreta un archivo Excel (.xlsx) o CSV (.csv) y devuelve la lista de filas de stock a cargar.
     * Se esperan las columnas:
     * Columna A: Código de Barras | Columna B: Cantidad a Sumar | Columna C (Opcional): Nombre | Columna D (Opcional): Precio
     * También detecta automáticamente si el orden es:
     * Columna A: Código de Barras | Columna B: Nombre | Columna C: Cantidad a Sumar | Columna D: Precio
     *
     * @param archivo Archivo seleccionado por el usuario.
     * @return Lista de objetos FilaCargaStock interpretados.
     * @throws IOException Si el archivo no existe o tiene un formato inválido.
     */
    public List<FilaCargaStock> interpretarArchivo(File archivo) throws IOException {
        if (archivo == null || !archivo.exists()) {
            throw new IOException("El archivo seleccionado no existe.");
        }
        String nombreMinuscula = archivo.getName().toLowerCase();
        if (nombreMinuscula.endsWith(".xlsx")) {
            return leerPlanillaXlsx(archivo);
        } else if (nombreMinuscula.endsWith(".csv") || nombreMinuscula.endsWith(".txt")) {
            return leerPlanillaCsv(archivo);
        } else {
            throw new IOException("Formato no soportado. Seleccione una planilla Excel (.xlsx) o (.csv).");
        }
    }

    /**
     * Lee de forma imperativa una planilla Microsoft Excel (.xlsx) inspeccionando su estructura XML interna.
     */
    private List<FilaCargaStock> leerPlanillaXlsx(File archivoXlsx) throws IOException {
        List<FilaCargaStock> resultado = new ArrayList<FilaCargaStock>();
        ZipFile zip = null;
        try {
            zip = new ZipFile(archivoXlsx);
            List<String> sharedStrings = new ArrayList<String>();

            // 1. Leer xl/sharedStrings.xml si existe en el libro Excel
            ZipEntry entryShared = zip.getEntry("xl/sharedStrings.xml");
            if (entryShared != null) {
                InputStream isShared = zip.getInputStream(entryShared);
                Document docShared = parsearXmlSeguro(isShared);
                isShared.close();

                NodeList nodosSi = docShared.getElementsByTagName("si");
                for (int i = 0; i < nodosSi.getLength(); i++) {
                    Element elSi = (Element) nodosSi.item(i);
                    NodeList nodosT = elSi.getElementsByTagName("t");
                    StringBuilder sbTexto = new StringBuilder();
                    for (int j = 0; j < nodosT.getLength(); j++) {
                        sbTexto.append(nodosT.item(j).getTextContent());
                    }
                    sharedStrings.add(sbTexto.toString().trim());
                }
            }

            // 2. Leer la primera hoja de trabajo (xl/worksheets/sheet1.xml)
            ZipEntry entrySheet = zip.getEntry("xl/worksheets/sheet1.xml");
            if (entrySheet == null) {
                throw new IOException("No se encontró la hoja principal (sheet1.xml) dentro del archivo .xlsx.");
            }

            InputStream isSheet = zip.getInputStream(entrySheet);
            Document docSheet = parsearXmlSeguro(isSheet);
            isSheet.close();

            NodeList filasXml = docSheet.getElementsByTagName("row");
            for (int r = 0; r < filasXml.getLength(); r++) {
                Element filaEl = (Element) filasXml.item(r);
                int numFilaExcel = r + 1;
                String atrR = filaEl.getAttribute("r");
                if (atrR != null && !atrR.isEmpty()) {
                    try {
                        numFilaExcel = Integer.parseInt(atrR);
                    } catch (NumberFormatException exIgnorado) {
                        numFilaExcel = r + 1;
                    }
                }

                // Extraer hasta 6 columnas (A..F) de la fila actual
                String[] valoresCols = new String[] {"", "", "", "", "", ""};
                NodeList celdasXml = filaEl.getElementsByTagName("c");

                for (int c = 0; c < celdasXml.getLength(); c++) {
                    Element celdaEl = (Element) celdasXml.item(c);
                    String refCelda = celdaEl.getAttribute("r"); // Ej: "A5", "B5"
                    int indiceCol = obtenerIndiceColumnaDesdeReferencia(refCelda, c);
                    if (indiceCol >= 0 && indiceCol < valoresCols.length) {
                        valoresCols[indiceCol] = extraerValorCeldaXlsx(celdaEl, sharedStrings);
                    }
                }

                FilaCargaStock filaInterpretada = construirFilaDesdeColumnas(numFilaExcel, valoresCols);
                if (filaInterpretada != null) {
                    resultado.add(filaInterpretada);
                }
            }
        } catch (Exception e) {
            throw new IOException("Error al interpretar la planilla Excel (.xlsx): " + e.getMessage(), e);
        } finally {
            if (zip != null) {
                zip.close();
            }
        }
        return resultado;
    }

    /**
     * Lee de forma imperativa una planilla CSV separada por punto y coma (;) o coma (,).
     */
    private List<FilaCargaStock> leerPlanillaCsv(File archivoCsv) throws IOException {
        List<FilaCargaStock> resultado = new ArrayList<FilaCargaStock>();
        BufferedReader lector = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivoCsv), StandardCharsets.UTF_8));
        try {
            String linea;
            int numLinea = 0;
            while ((linea = lector.readLine()) != null) {
                numLinea = numLinea + 1;
                String limpia = linea.trim();
                if (limpia.isEmpty()) {
                    continue;
                }
                String separador = limpia.contains(";") ? ";" : ",";
                String[] partes = limpia.split(separador);
                String[] valoresCols = new String[] {"", "", "", "", "", ""};
                for (int i = 0; i < partes.length && i < valoresCols.length; i++) {
                    valoresCols[i] = partes[i].replace("\"", "").trim();
                }

                FilaCargaStock filaInterpretada = construirFilaDesdeColumnas(numLinea, valoresCols);
                if (filaInterpretada != null) {
                    resultado.add(filaInterpretada);
                }
            }
        } finally {
            lector.close();
        }
        return resultado;
    }

    /**
     * Convierte un vector de columnas de texto en un objeto FilaCargaStock, ignorando
     * automáticamente encabezados de tabla, títulos o filas de ejemplo vacías.
     */
    private FilaCargaStock construirFilaDesdeColumnas(int numFila, String[] cols) {
        String colA = cols[0] != null ? cols[0].trim() : "";
        String colB = cols[1] != null ? cols[1].trim() : "";
        String colC = cols[2] != null ? cols[2].trim() : "";
        String colD = cols[3] != null ? cols[3].trim() : "";

        if (colA.isEmpty()) {
            return null;
        }

        // Si el código de barras fue guardado como notación decimal por Excel (ej. "7790001001015.0"), limpiarlo
        if (colA.endsWith(".0")) {
            colA = colA.substring(0, colA.length() - 2);
        }

        // Ignorar encabezados, banners de instrucciones o filas de totales
        String colAMinus = colA.toLowerCase();
        String colBMinus = colB.toLowerCase();
        if (colAMinus.contains("codigo") || colAMinus.contains("código")
                || colAMinus.contains("planilla") || colAMinus.contains("instrucc")
                || colAMinus.contains("total") || colAMinus.contains("nota")
                || colBMinus.contains("total") || colBMinus.contains("lote")) {
            return null;
        }

        // Verificar que la columna A comience con un dígito o letra de código válido
        char primerChar = colA.charAt(0);
        if (!Character.isDigit(primerChar)) {
            return null;
        }

        // Determinar si la Cantidad a Sumar está en la Columna B (formato corto: Codigo | Cantidad)
        // o en la Columna C (formato descriptivo: Codigo | Nombre | Cantidad | Precio)
        int cantidad = 0;
        String nombre = "";
        String categoria = "Almacén";
        double precioOpcional = 0.0;

        Integer cantEnB = intentarParsearEntero(colB);
        Integer cantEnC = intentarParsearEntero(colC);

        if (cantEnB != null && (cantEnC == null || colC.isEmpty() || !esNumeroEnteroPuro(colC))) {
            // Formato: [A: Código] [B: Cantidad] [C: Nombre opcional] [D: Precio opcional]
            cantidad = cantEnB.intValue();
            nombre = colC;
            precioOpcional = intentarParsearDouble(colD);
        } else if (cantEnC != null) {
            // Formato: [A: Código] [B: Nombre] [C: Cantidad] [D: Precio opcional]
            nombre = colB;
            cantidad = cantEnC.intValue();
            precioOpcional = intentarParsearDouble(colD);
        } else {
            return null;
        }

        if (cantidad <= 0) {
            return null;
        }

        return new FilaCargaStock(numFila, colA, nombre, categoria, cantidad, precioOpcional, "PENDIENTE");
    }

    private boolean esNumeroEnteroPuro(String texto) {
        if (texto == null || texto.isEmpty()) {
            return false;
        }
        String limpio = texto.endsWith(".0") ? texto.substring(0, texto.length() - 2) : texto;
        for (int i = 0; i < limpio.length(); i++) {
            if (!Character.isDigit(limpio.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private Integer intentarParsearEntero(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        try {
            double valorDouble = Double.parseDouble(texto.trim().replace(",", "."));
            return Integer.valueOf((int) Math.round(valorDouble));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double intentarParsearDouble(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return 0.0;
        }
        try {
            String limpio = texto.replace("$", "").replace(" ", "").trim();
            return Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int obtenerIndiceColumnaDesdeReferencia(String refCelda, int indicePorDefecto) {
        if (refCelda == null || refCelda.isEmpty()) {
            return indicePorDefecto;
        }
        int col = 0;
        for (int i = 0; i < refCelda.length(); i++) {
            char ch = refCelda.charAt(i);
            if (Character.isLetter(ch)) {
                col = col * 26 + (Character.toUpperCase(ch) - 'A' + 1);
            } else {
                break;
            }
        }
        if (col > 0) {
            return col - 1;
        }
        return indicePorDefecto;
    }

    private String extraerValorCeldaXlsx(Element celdaEl, List<String> sharedStrings) {
        String tipo = celdaEl.getAttribute("t");
        if ("inlineStr".equals(tipo)) {
            NodeList nodosT = celdaEl.getElementsByTagName("t");
            if (nodosT.getLength() > 0) {
                return nodosT.item(0).getTextContent().trim();
            }
            return "";
        }

        NodeList nodosV = celdaEl.getElementsByTagName("v");
        if (nodosV.getLength() == 0) {
            return "";
        }
        String valorCrudo = nodosV.item(0).getTextContent().trim();

        if ("s".equals(tipo)) {
            try {
                int idx = Integer.parseInt(valorCrudo);
                if (idx >= 0 && idx < sharedStrings.size()) {
                    return sharedStrings.get(idx);
                }
            } catch (NumberFormatException e) {
                return valorCrudo;
            }
        }
        return valorCrudo;
    }

    private Document parsearXmlSeguro(InputStream is) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(is);
    }
}
