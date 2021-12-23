package com.example.terdrawerlayout;


public enum ViewState {
    FILL{
        @Override
        public int getTop(TertiaryDrawerLayout hoverView) {
            return hoverView.getMeasuredHeight()-hoverView.getTopStateFill();
        }

        @Override
        public int getHeight(TertiaryDrawerLayout hoverView) {
            return hoverView.getTopStateFill();
        }
    },       // 全屏

    HOVER{
        @Override
        public int getTop(TertiaryDrawerLayout hoverView) {
            return hoverView.getMeasuredHeight()-hoverView.getTopStateHover();
        }

        @Override
        public int getHeight(TertiaryDrawerLayout hoverView) {
            return hoverView.getTopStateHover();
        }
    },      // 半空悬停

    CLOSE{
        @Override
        public int getTop(TertiaryDrawerLayout hoverView) {
            return hoverView.getMeasuredHeight()-hoverView.getTopStateClose();
        }

        @Override
        public int getHeight(TertiaryDrawerLayout hoverView) {
            return hoverView.getTopStateClose();
        }
    };      // 关闭: 完全藏在屏幕底部

    public abstract int getTop(TertiaryDrawerLayout hoverView);
    public abstract int getHeight(TertiaryDrawerLayout hoverView);

}
