import com.vtlo.offmp3g.Recipe;

import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

/** Writes the Android app's recipe catalogue (Recipe.ALL) as JSON for the web app. */
public class ExportRecipes {
    public static void main(String[] args) throws Exception {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < Recipe.ALL.size(); i++) {
            Recipe r = Recipe.ALL.get(i);
            sb.append("  {").append(kv("id", r.id)).append(',').append(kv("section", r.section)).append(',')
                .append(kv("title", r.title)).append(',').append(kv("description", r.description)).append(',')
                .append(kv("original", r.original)).append(',').append(kv("outputKind", r.outputKind.name())).append(',')
                .append(kv("outputExt", r.outputExt)).append(',').append(kv("outputMime", r.outputMime)).append(',')
                .append("\"inputs\":[");
            for (int j = 0; j < r.inputs.size(); j++) {
                Recipe.Input in = r.inputs.get(j);
                if (j > 0) sb.append(',');
                sb.append('{').append(kv("key", in.key)).append(',').append(kv("label", in.label)).append(',')
                    .append(kv("mime", in.mime)).append(",\"multiple\":").append(in.multiple).append('}');
            }
            sb.append("],\"params\":[");
            for (int j = 0; j < r.params.size(); j++) {
                Recipe.Param p = r.params.get(j);
                if (j > 0) sb.append(',');
                sb.append('{').append(kv("key", p.key)).append(',').append(kv("label", p.label)).append(',')
                    .append(kv("default", p.defaultValue)).append('}');
            }
            sb.append("],\"steps\":[");
            List<String> steps = r.steps;
            for (int j = 0; j < steps.size(); j++) {
                if (j > 0) sb.append(',');
                sb.append(str(steps.get(j)));
            }
            sb.append("]}").append(i < Recipe.ALL.size() - 1 ? ",\n" : "\n");
        }
        sb.append("]\n");
        try (Writer w = new FileWriter(args[0])) {
            w.write(sb.toString());
        }
    }

    private static String kv(String k, String v) {
        return str(k) + ":" + str(v);
    }

    private static String str(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
