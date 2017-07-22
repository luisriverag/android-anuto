package ch.logixisland.anuto.business.level;

import android.content.Context;

import java.io.InputStream;

import ch.logixisland.anuto.R;
import ch.logixisland.anuto.business.manager.GameListener;
import ch.logixisland.anuto.business.manager.GameManager;
import ch.logixisland.anuto.business.score.ScoreBoard;
import ch.logixisland.anuto.engine.logic.GameEngine;
import ch.logixisland.anuto.engine.render.Viewport;
import ch.logixisland.anuto.entity.enemy.EnemyFactory;
import ch.logixisland.anuto.entity.plateau.Plateau;
import ch.logixisland.anuto.entity.plateau.PlateauFactory;
import ch.logixisland.anuto.entity.tower.TowerFactory;
import ch.logixisland.anuto.util.data.EnemySettings;
import ch.logixisland.anuto.util.data.GameSettings;
import ch.logixisland.anuto.util.data.LevelDescriptor;
import ch.logixisland.anuto.util.data.PlateauDescriptor;
import ch.logixisland.anuto.util.data.TowerSettings;
import ch.logixisland.anuto.util.data.WavesDescriptor;

public class LevelLoader implements GameListener {

    private final Context mContext;
    private final GameEngine mGameEngine;
    private final Viewport mViewport;
    private final ScoreBoard mScoreBoard;
    private final PlateauFactory mPlateauFactory;

    private LevelInfo mLevelInfo;
    private GameSettings mGameSettings;
    private TowerSettings mTowerSettings;
    private EnemySettings mEnemySettings;
    private LevelDescriptor mLevelDescriptor;
    private WavesDescriptor mWavesDescriptor;
    private GameManager mGameManager;

    public LevelLoader(Context context, GameEngine gameEngine, ScoreBoard scoreBoard, GameManager gameManager, Viewport viewport,
                       PlateauFactory plateauFactory,
                       TowerFactory towerFactory, EnemyFactory enemyFactory) {
        mContext = context;
        mGameEngine = gameEngine;
        mViewport = viewport;
        mScoreBoard = scoreBoard;
        mPlateauFactory = plateauFactory;
        mGameManager = gameManager;

        try {
            mGameSettings = GameSettings.fromXml(mContext.getResources().openRawResource(R.raw.game_settings));
            mTowerSettings = TowerSettings.fromXml(mContext.getResources().openRawResource(R.raw.tower_settings));
            mEnemySettings = EnemySettings.fromXml(mContext.getResources().openRawResource(R.raw.enemy_settings));
            mWavesDescriptor = WavesDescriptor.fromXml(mContext.getResources().openRawResource(R.raw.waves));
        } catch (Exception e) {
            throw new RuntimeException("Could not load settings!", e);
        }

        towerFactory.setTowerSettings(mTowerSettings);
        enemyFactory.setEnemySettings(mEnemySettings);
        mGameManager.addListener(this);
    }

    public LevelInfo getLevelInfo() {
        return mLevelInfo;
    }

    public GameSettings getGameSettings() {
        return mGameSettings;
    }

    public TowerSettings getTowerSettings() {
        return mTowerSettings;
    }

    public EnemySettings getEnemySettings() {
        return mEnemySettings;
    }

    public LevelDescriptor getLevelDescriptor() {
        return mLevelDescriptor;
    }

    public WavesDescriptor getWavesDescriptor() {
        return mWavesDescriptor;
    }

    public void loadLevel(final LevelInfo levelInfo) {
        if (mGameEngine.isThreadChangeNeeded()) {
            mGameEngine.post(new Runnable() {
                @Override
                public void run() {
                    loadLevel(levelInfo);
                }
            });
            return;
        }

        if (mLevelInfo == levelInfo) {
            return;
        }

        mLevelInfo = levelInfo;

        try {
            InputStream inputStream = mContext.getResources().openRawResource(mLevelInfo.getLevelDataResId());
            mLevelDescriptor = LevelDescriptor.fromXml(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Could not load level!", e);
        }

        mGameManager.restart();
    }

    @Override
    public void gameRestart() {
        mGameEngine.clear();

        for (PlateauDescriptor descriptor : mLevelDescriptor.getPlateaus()) {
            Plateau p = mPlateauFactory.createPlateau(descriptor.getName());
            p.setPosition(descriptor.getPosition());
            mGameEngine.add(p);
        }

        mViewport.setGameSize(mLevelDescriptor.getWidth(), mLevelDescriptor.getHeight());
        mScoreBoard.reset(mGameSettings.getLives(), mGameSettings.getCredits());
    }

    @Override
    public void gameOver() {

    }

}
