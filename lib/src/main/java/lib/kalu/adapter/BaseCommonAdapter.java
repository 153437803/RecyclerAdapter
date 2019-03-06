package lib.kalu.adapter;

import android.animation.Animator;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import lib.kalu.adapter.animation.AlphaInAnimation;
import lib.kalu.adapter.animation.BaseAnimation;
import lib.kalu.adapter.animation.ScaleInAnimation;
import lib.kalu.adapter.animation.SlideInBottomAnimation;
import lib.kalu.adapter.animation.SlideInLeftAnimation;
import lib.kalu.adapter.animation.SlideInRightAnimation;
import lib.kalu.adapter.holder.RecyclerHolder;
import lib.kalu.adapter.model.TransModel;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * description: 没有加载更多
 * created by kalu on 2017/5/26 14:22
 */
public abstract class BaseCommonAdapter<T> extends RecyclerView.Adapter<RecyclerHolder> {

    protected static final String TAG = BaseCommonAdapter.class.getSimpleName();
    private final Interpolator mInterpolator = new LinearInterpolator();
    protected int mLastPosition = -1;
    // 布局ID
    protected LinearLayout mHeaderLayout, mFooterLayout;
    protected FrameLayout mEmptyLayout;
    // 是否仅仅第一次加载显示动画
    private boolean isOpenAnimFirstOnly = true;
    // 显示动画
    private boolean isOpenAnim = false;
    // 动画显示时间
    private int mAnimTime = 300;
    private BaseAnimation mSelectAnimation = new AlphaInAnimation();

    /***********************************       方法API       **************************************/

    protected int onMerge(int position) {
        return 1;
    }

    public T getModel(@IntRange(from = 0) int position) {
        return position < onData().size() ? onData().get(position) : null;
    }

    protected int getItemModelType(int position) {
        return super.getItemViewType(position);
    }

    protected RecyclerHolder createModelHolder(ViewGroup parent, int viewType) {
        final View itemView = LayoutInflater.from(parent.getContext().getApplicationContext()).inflate(onView(), parent, false);
        return createSimpleHolder(parent, itemView);
    }

    protected RecyclerHolder createSimpleHolder(ViewGroup parent, View view) {

        if (null == view)
            return null;

        Class clazz = getClass();
        Class z = null;
        while (null == z && null != clazz) {
            z = createClass(clazz);
            clazz = clazz.getSuperclass();
        }
        RecyclerHolder k = createHolder(z, parent, view);
        return null != k ? k : new RecyclerHolder(parent, view);
    }

    private Class createClass(Class z) {
        Type type = z.getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType) type).getActualTypeArguments();
            for (Type temp : types) {
                if (temp instanceof Class) {
                    Class tempClass = (Class) temp;
                    if (RecyclerHolder.class.isAssignableFrom(tempClass)) {
                        return tempClass;
                    }
                }
            }
        }
        return null;
    }

    private final RecyclerHolder createHolder(Class z, @NonNull ViewGroup parent, @NonNull View view) {
        try {
            Constructor constructor;
            String buffer = Modifier.toString(z.getModifiers());
            String className = z.getName();
            if (className.contains("$") && !buffer.contains("static")) {
                constructor = z.getDeclaredConstructor(getClass(), ViewGroup.class, View.class);
                return (RecyclerHolder) constructor.newInstance(this, parent, view);
            } else {
                constructor = z.getDeclaredConstructor(ViewGroup.class, View.class);
                return (RecyclerHolder) constructor.newInstance(parent, view);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    protected void setModelStyle(RecyclerView.ViewHolder holder, boolean isModel) {

        if (isModel) {
            if (!isOpenAnim) return;
            if (!isOpenAnimFirstOnly || holder.getLayoutPosition() > mLastPosition) {
                for (Animator anim : mSelectAnimation.getAnimators(holder.itemView)) {
                    anim.setDuration(mAnimTime).start();
                    anim.setInterpolator(mInterpolator);
                }
                mLastPosition = holder.getLayoutPosition();
            }
        } else {

            if (null == holder || null == holder.itemView) return;

            final ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
            if (null == layoutParams) return;

            final boolean isStaggeredGridLayoutManager = (layoutParams instanceof StaggeredGridLayoutManager.LayoutParams);
            if (!isStaggeredGridLayoutManager) return;

            StaggeredGridLayoutManager.LayoutParams params = (StaggeredGridLayoutManager.LayoutParams) layoutParams;
            params.setFullSpan(true);
        }
    }

    protected boolean isModelType(int type) {
        return type != RecyclerHolder.HEAD_VIEW && type != RecyclerHolder.FOOT_VIEW && type != RecyclerHolder.NULL_VIEW;
    }

    /***********************************       重写API       **************************************/

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        final int size = onData().size();
        return size == 0 ? getNullCount() : size + getNullCount() + getHeadCount() + getFootCount();
    }

    @Override
    public int getItemViewType(int position) {

        // 没有数据
        if (null == onData() || onData().size() == 0) {
            return RecyclerHolder.NULL_VIEW;
        }
        // 有数据
        else {
            int numHead = getHeadCount();
            if (position < numHead) {
                return RecyclerHolder.HEAD_VIEW;
            } else {
                // 需要传递的索引位置
                int realPosition = position - numHead;
                int numModel = onData().size();
                return realPosition < numModel ? getItemModelType(realPosition) : RecyclerHolder.FOOT_VIEW;
            }
        }
    }

    @Override
    public RecyclerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerHolder holder;
        switch (viewType) {
            case RecyclerHolder.NULL_VIEW:
                holder = createSimpleHolder(parent, mEmptyLayout);
                break;
            case RecyclerHolder.HEAD_VIEW:
                holder = createSimpleHolder(parent, mHeaderLayout);
                break;
            case RecyclerHolder.FOOT_VIEW:
                holder = createSimpleHolder(parent, mFooterLayout);
                break;
            default:
                holder = createModelHolder(parent, viewType);
        }

        onHolder(holder.getLayoutManager(), holder, viewType);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerHolder holder, int position) {

        int itemViewType = holder.getItemViewType();
        if (itemViewType != RecyclerHolder.HEAD_VIEW && itemViewType != RecyclerHolder.NULL_VIEW && itemViewType != RecyclerHolder.FOOT_VIEW) {

            ViewGroup parent = holder.getParent();
            if (null == parent) return;

            RecyclerView view = (RecyclerView) parent;
            int id = view.getId();

            Object tag = view.getTag(id);
            if (null == tag) {
                Log.e("list", "11");
                view.setTag(id, true);

                view.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);

                        onRoll(newState != 0, false);
                        Log.e("list", "roll = " + newState);
                    }
                });
            } else {
                Log.e("list", "22");
            }

            int realPosition = holder.getLayoutPosition() - getHeadCount();
            onNext(holder, onData().get(realPosition), position);
        }
    }

    @Override
    public void onViewAttachedToWindow(RecyclerHolder holder) {
        super.onViewAttachedToWindow(holder);

        if (null == holder)
            return;

        final int viewType = holder.getItemViewType();
        setModelStyle(holder, viewType != RecyclerHolder.NULL_VIEW);
    }

    @Override
    public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (!(manager instanceof GridLayoutManager)) return;

        final GridLayoutManager gridManager = ((GridLayoutManager) manager);
        gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {

                int type = getItemViewType(position);
                final boolean modelType = isModelType(type);
                return modelType ? onMerge(position - getHeadCount()) : gridManager.getSpanCount();
            }
        });
    }

    public final void setLoadAnimation(@AnimationType int animationType, int animTime, boolean isOpenAnimFirstOnly) {
        this.isOpenAnim = true;
        this.isOpenAnimFirstOnly = isOpenAnimFirstOnly;
        this.mAnimTime = animTime;

        switch (animationType) {
            case BaseAnimation.ALPHAIN:
                mSelectAnimation = new AlphaInAnimation();
                break;
            case BaseAnimation.SCALEIN:
                mSelectAnimation = new ScaleInAnimation();
                break;
            case BaseAnimation.SLIDEIN_BOTTOM:
                mSelectAnimation = new SlideInBottomAnimation();
                break;
            case BaseAnimation.SLIDEIN_LEFT:
                mSelectAnimation = new SlideInLeftAnimation();
                break;
            case BaseAnimation.SLIDEIN_RIGHT:
                mSelectAnimation = new SlideInRightAnimation();
                break;
            default:
                break;
        }
    }

    /***********************************       展开API       **************************************/

    public void expand(@IntRange(from = 0) int position, boolean animate) {
        position -= getHeadCount();

        final T model = getModel(position);
        if (null == model || !(model instanceof TransModel)) return;

        TransModel trans = (TransModel) model;
        if (trans.isExpanded()) return;

        final List tempList = trans.getModelList();
        if (null == tempList || tempList.size() == 0) return;

        // 需要展开
        trans.setExpanded(true);
        final int tempSize = tempList.size();
        final int tempBegin = position + 1;
        onData().addAll(tempBegin, tempList);

        if (animate) {
            notifyItemRangeInserted(tempBegin, tempSize);
        } else {
            notifyDataSetChanged();
        }
    }

    public void expand(@IntRange(from = 0) int position) {
        expand(position, true);
    }

    public void expandAll() {
        for (int i = onData().size() - 1; i >= 0 + getHeadCount(); i--) {
            expand(i, true);
        }
    }

    /***********************************       折叠API       **************************************/

    public void collapse(@IntRange(from = 0) int position, boolean animate) {
        position -= getHeadCount();

        final T model = getModel(position);
        if (null == model || !(model instanceof TransModel)) return;

        TransModel trans = (TransModel) model;
        if (!trans.isExpanded()) return;

        final List<T> tempList = trans.getModelList();
        if (null == tempList || tempList.size() == 0) return;

        // 需要折叠
        trans.setExpanded(false);
        final int tempSize = tempList.size();
        final int tempBegin = position + 1;
        for (int i = 0; i < tempSize; i++) {
            onData().remove(tempBegin);
        }

        if (animate) {
            notifyItemRangeRemoved(tempBegin, tempSize);
        } else {
            notifyDataSetChanged();
        }
    }

    public void collapse(@IntRange(from = 0) int position) {
        collapse(position, true);
    }

    public void collapseAll() {
        for (int i = onData().size() - 1; i >= 0 + getHeadCount(); i--) {
            collapse(i, true);
        }
    }

    /***********************************       索引API       **************************************/

    public int getParentPosition(@NonNull T item) {

        if (null == item || null == onData() || onData().isEmpty()) return -1;

        int position = onData().indexOf(item);
        if (position == -1) return -1;

        int level = (item instanceof TransModel) ? ((TransModel) item).getLevel() : Integer.MAX_VALUE;

        if (level == 0) return position;
        if (level == -1) return -1;

        for (int i = position; i >= 0; i--) {
            T temp = onData().get(i);
            if (!(temp instanceof TransModel)) continue;
            TransModel expandable = (TransModel) temp;
            if (expandable.getLevel() >= 0 && expandable.getLevel() < level) return i;
        }
        return -1;
    }

    public View getViewPosition(RecyclerView recyclerView, int position, @IdRes int viewId) {

        if (recyclerView == null) return null;

        RecyclerHolder viewHolder = (RecyclerHolder) recyclerView.findViewHolderForLayoutPosition(position);
        if (viewHolder == null) return null;

        return viewHolder.getView(viewId);
    }

    /***********************************       头部API       **************************************/

    private int getHeadPosition() {
        return getHeadCount() == 1 ? -1 : 0;
    }

    public int getHeadCount() {
        return (mHeaderLayout == null || mHeaderLayout.getChildCount() == 0) ? 0 : 1;
    }

    public LinearLayout getHead() {
        return mHeaderLayout;
    }

    public void addHead(View header) {
        addHead(header, -1);
    }

    public void addHead(View header, int index) {
        addHead(header, index, LinearLayout.VERTICAL);
    }

    public void addHead(View header, int index, int orientation) {
        if (mHeaderLayout == null) {
            mHeaderLayout = new LinearLayout(header.getContext());
            if (orientation == LinearLayout.VERTICAL) {
                mHeaderLayout.setOrientation(LinearLayout.VERTICAL);
                mHeaderLayout.setLayoutParams(new RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            } else {
                mHeaderLayout.setOrientation(LinearLayout.HORIZONTAL);
                mHeaderLayout.setLayoutParams(new RecyclerView.LayoutParams(WRAP_CONTENT, MATCH_PARENT));
            }
        }
        final int childCount = mHeaderLayout.getChildCount();
        if (index < 0 || index > childCount) {
            index = childCount;
        }
        mHeaderLayout.addView(header, index);
        if (mHeaderLayout.getChildCount() == 1) {
            int position = getHeadPosition();
            if (position != -1) {
                notifyItemInserted(position);
            }
        }
    }

    public void changeHead(View header) {
        changeHead(header, 0, LinearLayout.VERTICAL);
    }

    public void changeHead(View header, int index) {
        changeHead(header, index, LinearLayout.VERTICAL);
    }

    public void changeHead(View header, int index, int orientation) {
        if (mHeaderLayout == null || mHeaderLayout.getChildCount() <= index) {
            addHead(header, index, orientation);
        } else {
            mHeaderLayout.removeViewAt(index);
            mHeaderLayout.addView(header, index);
        }
    }

    public void removeHead(View header) {
        if (getHeadCount() == 0) return;

        mHeaderLayout.removeView(header);
        if (mHeaderLayout.getChildCount() == 0) {
            int position = getHeadPosition();
            if (position != -1) {
                notifyItemRemoved(position);
            }
        }
    }

    public void removeAllHead() {
        if (getHeadCount() == 0) return;

        mHeaderLayout.removeAllViews();
        int position = getHeadPosition();
        if (position != -1) {
            notifyItemRemoved(position);
        }
    }

    /***********************************       尾部API       **************************************/

    private int getFootPosition() {

        final int footCount = getFootCount();
        if (footCount != 1) return -1;

        final int headCount = getHeadCount();
        if (headCount == 1) {
            return getHeadCount() + onData().size();
        }
        return onData().size();
    }

    public int getFootCount() {
        return (mFooterLayout == null || mFooterLayout.getChildCount() == 0) ? 0 : 1;
    }

    public LinearLayout getFoot() {
        return mFooterLayout;
    }

    public void addFoot(View footer) {
        addFoot(footer, -1, LinearLayout.VERTICAL);
    }

    public void addFoot(View footer, int index) {
        addFoot(footer, index, LinearLayout.VERTICAL);
    }

    public void addFoot(View footer, int index, int orientation) {
        if (mFooterLayout == null) {
            mFooterLayout = new LinearLayout(footer.getContext());
            if (orientation == LinearLayout.VERTICAL) {
                mFooterLayout.setOrientation(LinearLayout.VERTICAL);
                mFooterLayout.setLayoutParams(new RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            } else {
                mFooterLayout.setOrientation(LinearLayout.HORIZONTAL);
                mFooterLayout.setLayoutParams(new RecyclerView.LayoutParams(WRAP_CONTENT, MATCH_PARENT));
            }
        }
        final int childCount = mFooterLayout.getChildCount();
        if (index < 0 || index > childCount) {
            index = childCount;
        }
        mFooterLayout.addView(footer, index);
        if (mFooterLayout.getChildCount() == 1) {
            int position = getFootPosition();
            if (position != -1) {
                notifyItemInserted(position);
            }
        }
    }

    public void changeFoot(View header) {
        changeFoot(header, 0, LinearLayout.VERTICAL);
    }

    public void changeFoot(View header, int index) {
        changeFoot(header, index, LinearLayout.VERTICAL);
    }

    public void changeFoot(View header, int index, int orientation) {
        if (mFooterLayout == null || mFooterLayout.getChildCount() <= index) {
            addFoot(header, index, orientation);
        } else {
            mFooterLayout.removeViewAt(index);
            mFooterLayout.addView(header, index);
        }
    }

    public void removeFoot(View footer) {
        if (getFootCount() == 0) return;

        mFooterLayout.removeView(footer);
        if (mFooterLayout.getChildCount() == 0) {
            int position = getFootPosition();
            if (position != -1) {
                notifyItemRemoved(position);
            }
        }
    }

    public void removeAllFoot() {
        if (getFootCount() == 0) return;

        mFooterLayout.removeAllViews();
        int position = getFootPosition();
        if (position != -1) {
            notifyItemRemoved(position);
        }
    }

    /***********************************       空布局API      **************************************/

    public int getNullCount() {

        if (mEmptyLayout == null || mEmptyLayout.getChildCount() == 0) return 0;

        if (onData().size() != 0) return 0;
        return 1;
    }

    public void setNullView(Context context, int layoutResId) {
        View view = LayoutInflater.from(context).inflate(layoutResId, null, false);
        setNullView(view);
    }

    public View getNullView() {
        return mEmptyLayout;
    }


    // TODO: 2018/8/10
    public void setNullView(View emptyView) {

        if (null == mEmptyLayout) {
            mEmptyLayout = new FrameLayout(emptyView.getContext());
        }
        if (null == mEmptyLayout.getLayoutParams()) {
            mEmptyLayout.setLayoutParams(new ViewGroup.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT));
        }
        final ViewGroup.LayoutParams mEmptyParams = mEmptyLayout.getLayoutParams();
        mEmptyParams.width = RecyclerView.LayoutParams.MATCH_PARENT;
        mEmptyParams.height = RecyclerView.LayoutParams.MATCH_PARENT;
        mEmptyLayout.removeAllViews();
        mEmptyLayout.addView(emptyView);

        final int nullCount = getNullCount();
        if (nullCount != 1)
            return;

        int position = 0;
        if (getHeadCount() != 0) {
            position++;
        }
        notifyItemInserted(position);
    }

    public void removeNullView() {

        if (null == mEmptyLayout) return;
        mEmptyLayout.removeAllViews();
        mEmptyLayout.setVisibility(View.GONE);
    }

    /***********************************       数据API      **************************************/

    public void clearInsertData(@Nullable List<T> data) {

        onData().clear();
        mLastPosition = -1;
        onData().addAll(data);
        notifyDataSetChanged();
    }

    public void addData(@NonNull T data) {
        onData().add(data);
        notifyItemInserted(onData().size() + getHeadCount());
        notifyDataSetChanged();
    }

    public void addData(@NonNull Collection<? extends T> newData) {
        onData().addAll(newData);
        notifyItemRangeInserted(onData().size() - newData.size() + getHeadCount(), newData.size());
        notifyDataSetChanged();
    }

    public void remove(@IntRange(from = 0) int position) {
        onData().remove(position);
        int internalPosition = position + getHeadCount();
        notifyItemRemoved(internalPosition);
        notifyItemRangeChanged(internalPosition, onData().size() - internalPosition);
    }

    public void setData(@IntRange(from = 0) int index, @NonNull T data) {
        onData().set(index, data);
        notifyItemChanged(index + getHeadCount());
    }

    /**********************************       抽象方法API     **************************************/

    protected abstract @LayoutRes
    int onView();

    protected abstract @NonNull
    List<T> onData();

    protected abstract void onNext(RecyclerHolder holder, T model, int position);

    /**
     * @param roll     是否滚动结束
     * @param loadmore 是否加载更多
     */
    protected void onRoll(boolean roll, boolean loadmore) {
    }

    protected void onHolder(RecyclerView.LayoutManager manager, RecyclerHolder holder, int type) {
    }

    /***********************************       动画API       **************************************/

    @IntDef({BaseAnimation.ALPHAIN, BaseAnimation.SCALEIN, BaseAnimation.SLIDEIN_BOTTOM, BaseAnimation.SLIDEIN_LEFT, BaseAnimation.SLIDEIN_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AnimationType {
    }
}