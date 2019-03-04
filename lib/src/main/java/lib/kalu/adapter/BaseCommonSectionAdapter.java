package lib.kalu.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import lib.kalu.adapter.holder.RecyclerHolder;
import lib.kalu.adapter.model.SectionModel;

/**
 * description: 分组
 * created by kalu on 2017/5/26 14:54
 */
public abstract class BaseCommonSectionAdapter<T extends SectionModel> extends BaseCommonAdapter<T> {

    @Override
    protected int getItemModelType(int position) {
        return onData().get(position).isSection() ? RecyclerHolder.SECTION_VIEW : 0;
    }

    @Override
    protected RecyclerHolder createModelHolder(ViewGroup parent, int viewType) {
        if (viewType == RecyclerHolder.SECTION_VIEW) {
            final View inflate = LayoutInflater.from(parent.getContext().getApplicationContext()).inflate(onHead(), parent, false);
            return createSimpleHolder(parent, inflate);
        }

        return super.createModelHolder(parent, viewType);
    }

    @Override
    protected boolean isModelType(int type) {
        return super.isModelType(type) && (type != RecyclerHolder.SECTION_VIEW);
    }

    /**********************************       抽象方法API     **************************************/

    protected abstract @LayoutRes
    int onHead();
}
