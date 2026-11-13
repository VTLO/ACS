package com.vtlo.offmp3g;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/** Short animated guide in French, shown once at first launch (and from the help button). */
public class OnboardingActivity extends AppCompatActivity {

    private static final String PREFS = "onboarding";
    private static final String DONE = "done_v1";

    /** True until the guide has been finished or skipped once. */
    static boolean shouldShow(Context context) {
        return !context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(DONE, false);
    }

    private static void markDone(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putBoolean(DONE, true).apply();
    }

    private final List<Scene> scenes = new ArrayList<>();
    private ViewPager2 pager;
    private LinearLayout dots;
    private MaterialButton next;
    private int current = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        scenes.add(welcome());
        scenes.add(recipes());
        scenes.add(files());
        scenes.add(run());
        scenes.add(result());

        pager = findViewById(R.id.pager);
        dots = findViewById(R.id.dots);
        next = findViewById(R.id.next);
        pager.setAdapter(new PageAdapter());
        pager.setOffscreenPageLimit(1);
        for (int i = 0; i < scenes.size(); i++) {
            View dot = new View(this);
            dots.addView(dot);
        }
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                select(position);
            }
        });
        findViewById(R.id.skip).setOnClickListener(v -> finishGuide());
        next.setOnClickListener(v -> {
            if (pager.getCurrentItem() < scenes.size() - 1) {
                pager.setCurrentItem(pager.getCurrentItem() + 1, true);
            } else {
                finishGuide();
            }
        });
        select(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (current >= 0) scenes.get(current).start();
    }

    @Override
    protected void onPause() {
        if (current >= 0) scenes.get(current).stop();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (pager.getCurrentItem() > 0) {
            pager.setCurrentItem(pager.getCurrentItem() - 1, true);
        } else {
            finishGuide();
        }
    }

    private void finishGuide() {
        markDone(this);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void select(int position) {
        if (current >= 0 && current != position) scenes.get(current).stop();
        current = position;
        scenes.get(position).start();
        boolean last = position == scenes.size() - 1;
        next.setText(last ? "C'est parti" : "Suivant");
        findViewById(R.id.skip).setVisibility(last ? View.INVISIBLE : View.VISIBLE);
        for (int i = 0; i < dots.getChildCount(); i++) {
            View dot = dots.getChildAt(i);
            boolean active = i == position;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(active ? 24 : 8), dp(8));
            lp.setMargins(dp(4), 0, dp(4), 0);
            dot.setLayoutParams(lp);
            dot.setBackground(pill(color(active ? R.color.green : R.color.surface_high), dp(4)));
        }
    }

    // --- scenes ---------------------------------------------------------------------------------

    /** One guide step: an illustration with looping animations, a title and a text. */
    private final class Scene {
        final String title, text;
        final FrameLayout art;
        final List<Animator> animators = new ArrayList<>();
        Runnable reset = () -> { };

        Scene(String title, String text) {
            this.title = title;
            this.text = text;
            art = new FrameLayout(OnboardingActivity.this);
        }

        Animator loop(Animator a) {
            if (a instanceof ValueAnimator) {
                ((ValueAnimator) a).setRepeatCount(ValueAnimator.INFINITE);
            }
            animators.add(a);
            return a;
        }

        void start() {
            stop();
            reset.run();
            for (Animator a : animators) a.start();
        }

        void stop() {
            for (Animator a : animators) a.cancel();
        }
    }

    private Scene welcome() {
        Scene s = new Scene("Bienvenue dans VTLOffmp3g",
            "Toutes les commandes FFmpeg de la cheatsheet, sur votre téléphone, sans connexion.");
        GaugeView ring = new GaugeView(this);
        ring.setStrokeDp(12);
        s.art.addView(ring, centered(dp(220), dp(220)));
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.mipmap.ic_launcher_round);
        s.art.addView(icon, centered(dp(110), dp(110)));

        ValueAnimator fill = ValueAnimator.ofFloat(0, 1);
        fill.setDuration(2200);
        fill.setInterpolator(new DecelerateInterpolator());
        fill.addUpdateListener(a -> ring.setProgress((float) a.getAnimatedValue()));
        s.loop(fill);
        ObjectAnimator pulseX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 1.08f, 1f);
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 1.08f, 1f);
        for (ObjectAnimator p : new ObjectAnimator[]{pulseX, pulseY}) {
            p.setDuration(1100);
            s.loop(p);
        }
        return s;
    }

    private Scene recipes() {
        Scene s = new Scene("Choisissez une recette",
            "Quatre onglets : Convertir, Audio, Montage, Images. Ou cherchez un format : mp3, gif, webm…");
        LinearLayout col = column();
        TextView search = text("", 17, color(R.color.search_text), false);
        search.setBackground(pill(color(R.color.search_bg), dp(4)));
        search.setPadding(dp(16), dp(12), dp(16), dp(12));
        col.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        String[][] rows = {{"MP4 → WebM (VP9 + Opus)", "WEBM"}, {"Extraire l'audio en MP3", "MP3"}, {"Vidéo → GIF optimisé", "GIF"}};
        List<View> rowViews = new ArrayList<>();
        for (String[] r : rows) {
            View row = mockRow(r[0], r[1]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(8);
            col.addView(row, lp);
            rowViews.add(row);
        }
        s.art.addView(col, centered(dp(300), ViewGroup.LayoutParams.WRAP_CONTENT));

        String query = "webm";
        ValueAnimator typing = ValueAnimator.ofInt(0, 12);
        typing.setDuration(3600);
        typing.addUpdateListener(a -> {
            int step = (int) a.getAnimatedValue();
            int n = Math.min(query.length(), Math.max(0, step - 2));
            search.setText(n == 0 ? "Rechercher…" : query.substring(0, n) + (step % 2 == 0 ? "|" : ""));
            search.setTextColor(n == 0 ? 0xFF8A8F8B : color(R.color.search_text));
            // Rows that do not match fade out once the word is typed.
            boolean filtered = n == query.length();
            for (int i = 1; i < rowViews.size(); i++) rowViews.get(i).setAlpha(filtered ? 0.25f : 1f);
        });
        s.loop(typing);
        for (int i = 0; i < rowViews.size(); i++) {
            View row = rowViews.get(i);
            ObjectAnimator slide = ObjectAnimator.ofFloat(row, View.TRANSLATION_X, dp(320), 0);
            slide.setDuration(500);
            slide.setStartDelay(150L * i);
            slide.setInterpolator(new DecelerateInterpolator());
            s.animators.add(slide);
        }
        return s;
    }

    private Scene files() {
        Scene s = new Scene("Ajoutez vos fichiers",
            "Touchez Choisir, puis réglez les paramètres. La commande FFmpeg reste modifiable.");
        LinearLayout col = column();
        LinearLayout card = column();
        card.setBackground(pill(color(R.color.surface), dp(24)));
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView chip = new ImageView(this);
        chip.setImageResource(R.drawable.ic_file);
        chip.setColorFilter(color(R.color.amber));
        chip.setBackgroundResource(R.drawable.bg_icon_chip);
        chip.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.addView(chip, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout labels = column();
        labels.setPadding(dp(12), 0, 0, 0);
        labels.addView(text("Vidéo source", 15, color(R.color.text), true));
        TextView name = text("aucun fichier", 11, color(R.color.text_dim), false);
        name.setTypeface(Typeface.MONOSPACE);
        labels.addView(name);
        row.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView pick = text("Choisir", 14, color(R.color.on_green), true);
        pick.setBackground(pill(color(R.color.green), dp(20)));
        pick.setPadding(dp(16), dp(8), dp(16), dp(8));
        row.addView(pick);
        card.addView(row);
        TextView param = text("CRF  23", 16, color(R.color.text), false);
        param.setBackground(pill(color(R.color.surface_high), dp(14)));
        param.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.topMargin = dp(14);
        card.addView(param, plp);
        col.addView(card);
        TextView cmd = text("ffmpeg -i vacances.mp4 -crf 23 out.mp4", 11, color(R.color.scope_yellow), false);
        cmd.setTypeface(Typeface.MONOSPACE);
        cmd.setBackground(Panels.scope(this));
        cmd.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(10);
        col.addView(cmd, clp);
        s.art.addView(col, centered(dp(300), ViewGroup.LayoutParams.WRAP_CONTENT));

        int[] crf = {18, 23, 28, 23};
        ValueAnimator story = ValueAnimator.ofFloat(0, 1);
        story.setDuration(4000);
        story.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            float press = t < 0.15f ? 1f - 0.12f * (float) Math.sin(t / 0.15f * Math.PI) : 1f;
            pick.setScaleX(press);
            pick.setScaleY(press);
            boolean picked = t >= 0.15f;
            name.setText(picked ? "vacances.mp4" : "aucun fichier");
            name.setTextColor(picked ? color(R.color.green) : color(R.color.text_dim));
            int value = crf[Math.min(crf.length - 1, (int) (Math.max(0, t - 0.3f) / 0.7f * crf.length))];
            param.setText("CRF  " + value);
            cmd.setText("ffmpeg -i vacances.mp4 -crf " + value + " out.mp4");
        });
        s.loop(story);
        return s;
    }

    private Scene run() {
        Scene s = new Scene("Lancez et suivez",
            "Touchez Lancer. La jauge et la télémétrie affichent la progression, les fps et le débit en direct.");
        LinearLayout col = column();
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(Panels.stripes(this));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        FrameLayout gaugeBox = new FrameLayout(this);
        GaugeView gauge = new GaugeView(this);
        gauge.setSweep(270);
        gauge.setStrokeDp(8);
        gauge.setColors(color(R.color.gauge_track), color(R.color.green));
        gaugeBox.addView(gauge, new FrameLayout.LayoutParams(dp(110), dp(110)));
        TextView score = text("0", 26, color(R.color.text), true);
        gaugeBox.addView(score, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        card.addView(gaugeBox);
        LinearLayout right = column();
        right.setPadding(dp(16), 0, 0, 0);
        TextView status = text("Encodage", 22, color(R.color.green), true);
        right.addView(status);
        TextView speed = text("1.8x", 14, color(R.color.on_amber), true);
        speed.setBackground(pill(color(R.color.amber), dp(8)));
        speed.setPadding(dp(10), dp(2), dp(10), dp(2));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(6);
        right.addView(speed, slp);
        card.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        col.addView(card, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ScopeView scope = new ScopeView(this);
        LinearLayout.LayoutParams sclp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(120));
        sclp.topMargin = dp(10);
        col.addView(scope, sclp);
        s.art.addView(col, centered(dp(310), ViewGroup.LayoutParams.WRAP_CONTENT));

        List<float[]> samples = new ArrayList<>();
        s.reset = samples::clear;
        ValueAnimator progress = ValueAnimator.ofFloat(0, 1.25f);
        progress.setDuration(5000);
        progress.addUpdateListener(a -> {
            float t = Math.min(1f, (float) a.getAnimatedValue());
            if (t < 0.02f) samples.clear();
            gauge.setProgress(t);
            score.setText(String.valueOf(Math.round(t * 100)));
            boolean done = t >= 1f;
            status.setText(done ? "Terminé" : "Encodage");
            speed.setText(done ? "12 Mo" : String.format(java.util.Locale.US, "%.1fx", 1.6 + 0.4 * Math.sin(t * 9)));
            int n = Math.round(t * 60);
            while (samples.size() < n) {
                int i = samples.size();
                float fps = 42 + 8 * (float) Math.sin(i / 4.0) + (i % 5) - (i < 4 ? (4 - i) * 8 : 0);
                float kbps = 1900 + 500 * (float) Math.cos(i / 7.0) + (i % 3) * 80;
                samples.add(new float[]{i * 0.8f, Math.max(0, fps), kbps});
            }
            scope.setSamples(new ArrayList<>(samples));
        });
        s.loop(progress);
        return s;
    }

    private Scene result() {
        Scene s = new Scene("Récupérez le résultat",
            "Enregistrez, partagez ou ouvrez le fichier. En cas de problème, un rapport s'affiche au lancement suivant.");
        LinearLayout col = column();
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView done = text("✓", 54, color(R.color.on_green), true);
        done.setGravity(Gravity.CENTER);
        done.setBackground(oval(color(R.color.green)));
        col.addView(done, new LinearLayout.LayoutParams(dp(96), dp(96)));
        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.CENTER);
        String[] labels = {"Enregistrer", "Partager", "Ouvrir"};
        List<View> pills = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            TextView b = text(labels[i], 13, color(i == 0 ? R.color.on_green : R.color.text), true);
            b.setBackground(pill(color(i == 0 ? R.color.green : R.color.surface_high), dp(20)));
            b.setPadding(dp(14), dp(10), dp(14), dp(10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(4), 0, dp(4), 0);
            buttons.addView(b, lp);
            pills.add(b);
        }
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(28);
        col.addView(buttons, blp);
        s.art.addView(col, centered(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AnimatorSet pop = new AnimatorSet();
        List<Animator> parts = new ArrayList<>();
        parts.add(scaleIn(done, 0));
        for (int i = 0; i < pills.size(); i++) parts.add(scaleIn(pills.get(i), 350 + 120L * i));
        pop.playTogether(parts);
        s.animators.add(pop);
        ObjectAnimator glow = ObjectAnimator.ofFloat(done, View.ALPHA, 1f, 0.75f, 1f);
        glow.setDuration(1600);
        glow.setStartDelay(900);
        s.loop(glow);
        return s;
    }

    // --- view helpers ---------------------------------------------------------------------------

    private Animator scaleIn(View v, long delay) {
        v.setScaleX(0);
        v.setScaleY(0);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(ObjectAnimator.ofFloat(v, View.SCALE_X, 0f, 1f), ObjectAnimator.ofFloat(v, View.SCALE_Y, 0f, 1f));
        set.setDuration(420);
        set.setStartDelay(delay);
        set.setInterpolator(new OvershootInterpolator(2f));
        return set;
    }

    private View mockRow(String title, String badge) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(pill(color(R.color.surface), dp(6)));
        row.setPadding(dp(16), dp(14), dp(12), dp(14));
        row.addView(text(title, 14, color(R.color.text), false),
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text(badge, 14, color(R.color.orange), true));
        ImageView chevron = new ImageView(this);
        chevron.setImageResource(R.drawable.ic_chevron);
        chevron.setColorFilter(color(R.color.text));
        row.addView(chevron, new LinearLayout.LayoutParams(dp(22), dp(22)));
        return row;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private static FrameLayout.LayoutParams centered(int w, int h) {
        return new FrameLayout.LayoutParams(w, h, Gravity.CENTER);
    }

    private android.graphics.drawable.GradientDrawable pill(int color, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private android.graphics.drawable.GradientDrawable oval(int color) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    private int color(int res) {
        return ContextCompat.getColor(this, res);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // --- pager ----------------------------------------------------------------------------------

    private final class PageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View page = getLayoutInflater().inflate(R.layout.page_onboarding, parent, false);
            return new RecyclerView.ViewHolder(page) { };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Scene s = scenes.get(position);
            FrameLayout art = holder.itemView.findViewById(R.id.art);
            art.removeAllViews();
            if (s.art.getParent() != null) ((ViewGroup) s.art.getParent()).removeView(s.art);
            art.addView(s.art, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ((TextView) holder.itemView.findViewById(R.id.pageTitle)).setText(s.title);
            ((TextView) holder.itemView.findViewById(R.id.pageText)).setText(s.text);
            ((TextView) holder.itemView.findViewById(R.id.pageStep)).setText(
                String.format(java.util.Locale.FRANCE, "ÉTAPE %d / %d", position + 1, scenes.size()));
        }

        @Override
        public int getItemCount() {
            return scenes.size();
        }
    }
}
