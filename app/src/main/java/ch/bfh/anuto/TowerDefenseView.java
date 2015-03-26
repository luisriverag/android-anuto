package ch.bfh.anuto;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;

import java.util.Iterator;

import ch.bfh.anuto.game.GameObject;
import ch.bfh.anuto.game.objects.Plateau;
import ch.bfh.anuto.game.objects.Tower;
import ch.bfh.anuto.game.GameEngine;
import ch.bfh.anuto.util.iterator.Iterators;
import ch.bfh.anuto.util.math.Vector2;

public class TowerDefenseView extends View implements GameEngine.Listener, View.OnDragListener, View.OnTouchListener {
    private final static String TAG = TowerDefenseView.class.getName();

    private GameEngine mGame;
    private Tower mSelectedTower;

    public TowerDefenseView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!isInEditMode()) {
            setFocusable(true);
            setOnDragListener(this);
            setOnTouchListener(this);
        }
    }

    public GameEngine getGame() {
        return mGame;
    }

    public void setGame(GameEngine game) {
        if (mGame != null) {
            mGame.removeListener(this);
        }

        mGame = game;

        if (mGame != null) {
            mGame.addListener(this);
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (!isInEditMode()) {
            mGame.setScreenSize(w, h);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isInEditMode() && mGame != null) {
            mGame.render(canvas);
        }
    }

    @Override
    public void onRenderRequest() {
        postInvalidate();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mSelectedTower != null) {
                mSelectedTower.hideRange();
                mSelectedTower = null;
            }

            Vector2 pos = mGame.getGameCoordinate(event.getX(), event.getY());
            Tower closest = (Tower)GameObject.closest(mGame.getGameObjects(Tower.TYPE_ID), pos);

            if (closest != null && closest.getDistanceTo(pos) < 0.5f) {
                mSelectedTower = closest;
                mSelectedTower.showRange();
            }
        }

        return false;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        Tower tower = (Tower)event.getLocalState();

        Iterator<Plateau> plateaus = Iterators.cast(mGame.getGameObjects(Plateau.TYPE_ID), Plateau.class);
        Iterator<Plateau> freePlateaus = Plateau.unoccupied(plateaus);

        Vector2 pos;
        Plateau plateau;

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_ENTERED:
                if (freePlateaus.hasNext()) {
                    mGame.addGameObject(tower);
                    tower.showRange();
                }
                break;

            case DragEvent.ACTION_DRAG_EXITED:
                mGame.removeGameObject(tower);
                break;

            case DragEvent.ACTION_DROP:
                pos = mGame.getGameCoordinate(event.getX(), event.getY());
                plateau = GameObject.closest(freePlateaus, pos);
                tower.setPosition(plateau);

                tower.hideRange();
                break;

            case DragEvent.ACTION_DRAG_LOCATION:
                pos = mGame.getGameCoordinate(event.getX(), event.getY());
                plateau = GameObject.closest(freePlateaus, pos);
                tower.setPosition(plateau.getPosition());
                break;
        }

        return true;
    }
}
