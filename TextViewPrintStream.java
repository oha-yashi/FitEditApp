package com.example.fiteditapp;

// https://blog.goo.ne.jp/marunomarunogoo/e/943906fc518b7c0c767c9804efb297e1

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 標準入出力を Log と テキストビューに出力させるようにするクラス.
 * 指定されたテキストビューは、スクロールビューに囲まれている必要がある。
 * @author marunomaruno
 * @version 1.0, 2011-12-14
 * @since 1.0
 */
public class TextViewPrintStream extends PrintStream { // (1) PrintStream クラスのサブクラスを作る

    private TextView view; // (2) 出力先のオブジェクト
    private Runnable scrollDown = new ScrollDown();    // (3) スクロールダウンさせるスレッドのオブジェクト

    public TextViewPrintStream(File file, String csn, TextView view)
            throws FileNotFoundException, UnsupportedEncodingException { // (4) コンストラクター
        super(file, csn);
        this.view = view;
        ((View) view.getParent()).post(scrollDown);    // (5) このテキストビューを囲むスクロールビュー・オブジェクトに対してつねに最下段を指すように指定
    }

    public TextViewPrintStream(File file, TextView view)
            throws FileNotFoundException {    // (4)
        super(file);
        this.view = view;
        ((View) view.getParent()).post(scrollDown);    // (5)
    }

    public TextViewPrintStream(OutputStream out, boolean autoFlush, String enc,
                               TextView view) throws UnsupportedEncodingException {    // (4)
        super(out, autoFlush, enc);
        this.view = view;
        ((View) view.getParent()).post(scrollDown);    // (5)
    }

    public TextViewPrintStream(OutputStream out, boolean autoFlush,
                               TextView view) {    // (4)
        super(out, autoFlush);
        this.view = view;
        ((View) view.getParent()).post(scrollDown);    // (5)
    }

    public TextViewPrintStream(OutputStream out, TextView view) {    // (4)
        super(out);
        this.view = view;
        ((View) view.getParent()).post(scrollDown);    // (5)
    }

    public TextViewPrintStream(String fileName, String csn, TextView view)
            throws FileNotFoundException, UnsupportedEncodingException { // (4)
        super(fileName, csn);
        this.view = view;
        ((View) view.getParent()).post(scrollDown);    // (5)
    }

    public TextViewPrintStream(String fileName, TextView view)
            throws FileNotFoundException {    // (4)
        super(fileName);
        this.view = view;
        ((View) view.getParent()).post(scrollDown);    // (5)
    }

    @Override
    public synchronized void print(String str) { // (6) print メソッドのオーバーライド
        super.print(str);
        view.append(str); // (7) TextView に文字列を追加する
    }

    @Override
    public synchronized void println(String str){
        super.println(str+"\n"); //よくわからんけどこれで動く
//        view.append(str+"\n");
    }

    /**
     * スクロールダウンさせるスレッドのクラス
     * @author marunomaruno
     */
    private class ScrollDown implements Runnable { // (8) スクロールさせるためのクラス
        public void run() {
            ((ScrollView) view.getParent()).fullScroll(View.FOCUS_DOWN); // (9) スクロールさせる
        }
    }
}
