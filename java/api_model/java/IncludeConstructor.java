package raml_framework.java;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;

/**
 * Created by antoniogarrote on 17/07/2016.
 */


public class IncludeConstructor extends Constructor {

    public IncludeConstructor(File fileToParse) {
        this.yamlConstructors.put(new Tag("!include"), new ConstructInclude(fileToParse));
    }

    private String readFile(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader (file));
        String         line = null;
        StringBuilder  stringBuilder = new StringBuilder();
        String         ls = System.getProperty("line.separator");

        try {
            while((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }

            return stringBuilder.toString();
        } finally {
            reader.close();
        }
    }

    private class ConstructInclude extends AbstractConstruct {
        private File fileToParse;

        public ConstructInclude(File fileToParse) {
            this.fileToParse = fileToParse;
        }

        public Object construct(Node node) {
            String val = (String) constructScalar((ScalarNode) node);

            File nestedFile = null;
            if(val.startsWith(File.separator)) {
                nestedFile = new File(val);
            } else {
                nestedFile = new File(fileToParse.getParentFile().getAbsolutePath()+File.separator+val);
            }

            IncludeConstructor nestedConstructor = new IncludeConstructor(nestedFile);
            Yaml yaml = new Yaml(nestedConstructor);

            try {
                // System.out.println(nestedFile);
                Object raw = yaml.load(new FileInputStream(nestedFile));
                String fragmentInfo = this.readFragmentInfo(nestedFile);
                LinkedHashMap fragment = new LinkedHashMap();
                fragment.put("@location", nestedFile.getAbsolutePath());
                if(raw instanceof LinkedHashMap) {
                    LinkedHashMap  parsed =  (LinkedHashMap) raw;
                    if (fragmentInfo != null) {
                        fragment.put("@fragment", fragmentInfo);
                        fragment.put("@data", parsed);
                    }

                } else {
                    fragment.put("@location", nestedFile.getAbsolutePath());
                    fragment.put("@data", raw);
                }
                return fragment;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private String readFragmentInfo(File file) throws IOException {
            BufferedReader reader = new BufferedReader(new FileReader (file));
            String         line = null;
            StringBuilder  stringBuilder = new StringBuilder();
            String         ls = System.getProperty("line.separator");

            try {
                line = reader.readLine();
                if (line != null) {
                    if (line.startsWith("#%RAML")) {
                        return line;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } finally {
                reader.close();
            }
        }
    }
}
