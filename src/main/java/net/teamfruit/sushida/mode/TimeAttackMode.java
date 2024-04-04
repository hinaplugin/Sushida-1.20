package net.teamfruit.sushida.mode;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.teamfruit.sushida.SoundManager;
import net.teamfruit.sushida.player.Group;
import net.teamfruit.sushida.player.StateContainer;
import net.teamfruit.sushida.util.CustomCollectors;
import net.teamfruit.sushida.util.SimpleTask;
import net.teamfruit.sushida.util.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TimeAttackMode implements GameMode {
    public static final GameSettingType SettingCount = new GameSettingType("count", "問題数", "問題数", 30, Arrays.asList(10, 20, 30, 60, 120));

    @Override
    public String title() {
        return "タイムアタック";
    }

    @Override
    public List<GameSettingType> getSettingTypes() {
        return Arrays.asList(SettingCount, SettingTimeout);
    }

    @Override
    public boolean isGameOver(StateContainer state) {
        return state.typingLogic.wordRemainingCount() <= 0;
    }

    @Override
    public String getScoreBelowName(StateContainer state) {
        return String.format(
                "%d点 (%d / %d)",
                getDynamicScore(state),
                state.typingLogic.wordDoneCount(),
                state.typingLogic.wordTotalCount()
        );
    }

    @Override
    public int getDynamicScore(StateContainer state) {
        int timeout = state.data.getGroup().getMode().getSetting(state.data.getGroup().getSettings(), SettingTimeout);
        if (timeout > 0)
            return state.clearCount * timeout - Math.round(state.timer.getTime());
        return state.clearCount;
    }

    @Override
    public int getScore(StateContainer state) {
        int timeout = state.data.getGroup().getMode().getSetting(state.data.getGroup().getSettings(), SettingTimeout);
        if (timeout > 0)
            return state.clearCount * timeout - Math.round(state.timer.getTime());
        return -Math.round(state.timer.getTime());
    }

    @Override
    public String getScoreString(StateContainer state) {
        return ChatColor.WHITE + "経過時間:"
                + ChatColor.GREEN + ChatColor.BOLD + String.format("%.0f秒", state.timer.getTime())
                + ChatColor.GRAY + ", "
                + ChatColor.WHITE + "残り枚数:"
                + ChatColor.GREEN + ChatColor.BOLD + String.format("%d皿", state.typingLogic.wordRemainingCount());
    }

    private final SimpleTask<StateContainer> messageTask = SimpleTask.<StateContainer>builder()
            .append(state -> {
                Player player = state.data.player;
                player.sendMessage("");
                player.sendMessage(new ComponentBuilder()
                        .append("  -------------------------").color(ChatColor.GRAY)
                        .create()
                );
                player.sendMessage("");
                int count = getSetting(state.data.getGroup().getSettings(), SettingCount);
                player.sendMessage(new ComponentBuilder()
                        .append("      ").color(ChatColor.WHITE)
                        .append("タイムアタックコース").color(ChatColor.BLUE)
                        .append(String.format(" 【%d皿】", count)).color(ChatColor.GOLD)
                        .create()
                );
                player.sendMessage("");
                player.sendMessage(new ComponentBuilder()
                        .append("  -------------------------").color(ChatColor.GRAY)
                        .create()
                );
                player.sendMessage("");
                player.sendMessage(new ComponentBuilder()
                        .append("    ▼成績").color(ChatColor.GOLD)
                        .create()
                );
            }).append(state -> {
                Player player = state.data.player;
                SoundManager.playSound(player, "sushida:sushida.cacher", SoundCategory.PLAYERS, 1, 1);
                player.sendMessage(new ComponentBuilder()
                        .append("      ").color(ChatColor.WHITE)
                        .append(String.valueOf(state.typingLogic.wordTotalCount())).color(ChatColor.GRAY)
                        .append(" 皿のうち・・・").color(ChatColor.GRAY)
                        .create()
                );
            }).append(state -> {
                Player player = state.data.player;
                SoundManager.playSound(player, "sushida:sushida.cacher", SoundCategory.PLAYERS, 1, 1);
                player.sendMessage(new ComponentBuilder()
                        .append("      ").color(ChatColor.WHITE)
                        .append(String.valueOf(state.clearCount)).color(ChatColor.YELLOW)
                        .append(" 皿分のお寿司をゲット！").color(ChatColor.WHITE)
                        .create()
                );
            }).append(state -> {
                Player player = state.data.player;
                SoundManager.playSound(player, "sushida:sushida.chin", SoundCategory.PLAYERS, 1, 1);
                player.sendMessage(new ComponentBuilder()
                        .append("      ").color(ChatColor.WHITE).underlined(false)
                        .append(String.format("%.1f", state.timer.getTime())).color(ChatColor.GREEN).underlined(true)
                        .append(" 秒かかりました！").color(ChatColor.WHITE).underlined(true)
                        .create()
                );
            }).append(state -> {
                Player player = state.data.player;
                player.sendMessage("");
                player.sendMessage(new ComponentBuilder()
                        .append("      正しく打ったキーの数: ").color(ChatColor.WHITE)
                        .append(String.valueOf(state.typeCount)).color(ChatColor.YELLOW)
                        .append(" 回").color(ChatColor.GRAY)
                        .create()
                );
                player.sendMessage(new ComponentBuilder()
                        .append("      平均キータイプ数: ").color(ChatColor.WHITE)
                        .append(String.format("%.1f", state.typeCount / state.realTimer.getTime())).color(ChatColor.YELLOW)
                        .append(" 回/秒").color(ChatColor.GRAY)
                        .create()
                );
                player.sendMessage(new ComponentBuilder()
                        .append("      ミスタイプ数: ").color(ChatColor.WHITE)
                        .append(String.valueOf(state.missCount)).color(ChatColor.YELLOW)
                        .append(" 回").color(ChatColor.GRAY)
                        .create()
                );
                int members = state.data.getGroup().getPlayers().size();
                if (members >= 2)
                    player.sendMessage(new ComponentBuilder()
                            .append("      ランキング: ").color(ChatColor.WHITE)
                            .append(String.valueOf(state.ranking)).color(ChatColor.YELLOW)
                            .append("位").color(ChatColor.GRAY)
                            .append("(").color(ChatColor.GRAY)
                            .append(String.valueOf(members)).color(ChatColor.WHITE)
                            .append("人中)").color(ChatColor.GRAY)
                            .create()
                    );
                // グローバルランキング算出
                state.data.getGroup().getRanking().ifPresent(ranking -> {
                    Scoreboard sc = Bukkit.getScoreboardManager().getMainScoreboard();
                    Objective objective = ranking.getOrCreateObjective(sc);
                    GameMode mode = state.data.getGroup().getMode();
                    Set<String> entries = sc.getEntries();
                    List<Integer> board = entries.stream()
                            .map(objective::getScore)
                            .filter(Score::isScoreSet)
                            .map(Score::getScore)
                            .sorted(mode.getScoreComparator())
                            .collect(Collectors.toList());
                    int myScore = objective.getScore(state.data.player.getName()).getScore();
                    int rank = board.indexOf(myScore) + 1;
                    player.sendMessage(new ComponentBuilder()
                            .append("      グローバルランキング: ").color(ChatColor.WHITE)
                            .append(String.valueOf(rank)).color(ChatColor.YELLOW)
                            .append("位").color(ChatColor.GRAY)
                            .append("(").color(ChatColor.GRAY)
                            .append(String.valueOf(board.size())).color(ChatColor.WHITE)
                            .append("人中)").color(ChatColor.GRAY)
                            .append(state.rankingUpdated
                                    ? new ComponentBuilder(" 自己新記録！").color(ChatColor.YELLOW).create()
                                    : new ComponentBuilder("").create())
                            .create()
                    );
                });

                player.sendMessage("");
            });

    @Override
    public Iterator<Consumer<StateContainer>> getResultMessageTasks() {
        return messageTask.build();
    }

    @Override
    public ImmutableList<Map.Entry<String, String>> getWords(Group group) {
        // 戻り値を作成するImmutableListビルダー
        ImmutableList.Builder<Map.Entry<String, String>> selectedWordsBuilder = ImmutableList.builder();

        // 設定
        int setting = getSetting(group.getSettings(), SettingCount);

        // 出題するレベルごとの単語数
        List<Integer> splits = CustomCollectors.splitInt(setting, group.getWord().mappings.size());

        // 単語をレベルごとに分けて取得
        List<Map.Entry<String, ImmutableList<ImmutableList<Map.Entry<String, String>>>>> wordRequiredListByLevel = group.getWord().mappings.entrySet().stream()
                .sorted(Comparator.comparingInt(a -> NumberUtils.toInt(CharMatcher.inRange('0', '9').retainFrom(a.getKey()), -1)))
                .collect(Collectors.toList());

        // レベルごとの単語リスト
        final Map<Integer, List<Map.Entry<String, String>>> wordMap = Maps.newTreeMap();

        /*
          List<Map.Entry<String, ImmutableList<ImmutableList<Map.Entry<String, String>>>>>から
          Map.Entry(String, String)を作成する
         */
        for (Map.Entry<String, ImmutableList<ImmutableList<Map.Entry<String, String>>>> key : wordRequiredListByLevel){
            final List<Map.Entry<String, String>> wordLevel = Lists.newArrayList();
            for (ImmutableList<Map.Entry<String, String>> imKey: key.getValue()){
                wordLevel.add(imKey.get(0));
            }
            final int lvl = wordMap.size();
            wordMap.put(lvl, wordLevel);
        }

        // 選択した単語
        List<Map.Entry<String, String>> results = new ArrayList<>();

        for (int idx = 0; idx < splits.size(); idx++) {
            int count = splits.get(idx);
            List<Map.Entry<String, String>> ls = new ArrayList<>(wordMap.get(idx));
            Collections.shuffle(ls);
            List<Map.Entry<String, String>> results_ = new ArrayList<>();

            for (Map.Entry<String, String> r : ls) {
                if (!results.contains(r) && !results_.contains(r)) {
                    results_.add(r);
                }
                if (results_.size() >= count) {
                    break;
                }
            }

            if (results_.size() < count) {
                for (int _idx = 0; _idx < wordMap.size(); _idx++) {
                    int n = (idx + _idx + 1) % wordMap.size();
                    List<Map.Entry<String, String>> otherLs = new ArrayList<>(wordMap.get(n));
                    Collections.shuffle(otherLs);
                    for (Map.Entry<String, String> r : otherLs) {
                        if (!results.contains(r) && !results_.contains(r)) {
                            results_.add(r);
                        }
                        if (results_.size() >= count) {
                            break;
                        }
                    }
                    if (results_.size() >= count) {
                        break;
                    }
                }
            }

            if (results_.size() < count) {
                List<Map.Entry<String, String>> allWords = new ArrayList<>();
                for (List<Map.Entry<String, String>> ls2 : wordMap.values()) {
                    allWords.addAll(ls2);
                }
                Collections.shuffle(allWords);
                results_.addAll(allWords.subList(0, count - results_.size()));
            }

            results.addAll(results_);
            for (Map.Entry<String, String> item : results_) {
                selectedWordsBuilder.add(item);
            }
        }
        return selectedWordsBuilder.build();
    }
}
