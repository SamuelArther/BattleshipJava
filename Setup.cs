using System;
using System.ComponentModel;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Windows.Forms;

internal static class Program
{
    [STAThread]
    static void Main()
    {
        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);
        Application.Run(new SetupWizard());
    }
}

internal sealed class SetupWizard : Form
{
    // ── Colours ───────────────────────────────────────────────
    static readonly Color NavyDark = Color.FromArgb(10,  25,  60);
    static readonly Color NavyMid  = Color.FromArgb(20,  55, 120);
    static readonly Color Gold     = Color.FromArgb(180, 140,  40);
    static readonly Color LightBg  = Color.FromArgb(236, 241, 252);

    // ── Layout constants ──────────────────────────────────────
    const int W        = 600;   // client width
    const int H        = 460;   // client height
    const int HeaderH  = 68;
    const int FooterH  = 48;
    const int ContentH = H - HeaderH - FooterH;  // 344
    const int Pad      = 24;
    const int ContentW = W - Pad * 2;            // 552

    // ── State ─────────────────────────────────────────────────
    private int    _page = 0;
    private string _srcDir;
    private string _dstDir;
    private bool   _createDesktop;
    private bool   _createStartMenu;
    private int    _lastPct = 0;

    // ── Chrome ────────────────────────────────────────────────
    private Label  _lblTitle, _lblSub;
    private Button _btnBack, _btnNext, _btnCancel;

    // ── Pages ─────────────────────────────────────────────────
    private Panel   _p0, _p1, _p2, _p3;
    private Panel[] _pages;

    // Page 1
    private TextBox  _txtDir;
    private CheckBox _chkDesktop, _chkStartMenu;

    // Page 2
    private ProgressBar    _bar;
    private RichTextBox    _log;
    private Label          _lblStatus;
    private BackgroundWorker _worker;

    // Page 3
    private CheckBox _chkLaunch;

    // ─────────────────────────────────────────────────────────
    public SetupWizard()
    {
        _srcDir = AppDomain.CurrentDomain.BaseDirectory.TrimEnd('\\', '/');
        _dstDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "BattleshipJava");

        InitForm();
        BuildHeader();
        BuildFooter();
        BuildPage0();
        BuildPage1();
        BuildPage2();
        BuildPage3();

        _pages = new[] { _p0, _p1, _p2, _p3 };
        ShowPage(0);
    }

    // ── Form ──────────────────────────────────────────────────
    void InitForm()
    {
        Text            = "BattleshipJava Setup";
        ClientSize      = new Size(W, H);
        FormBorderStyle = FormBorderStyle.FixedSingle;
        MaximizeBox     = false;
        StartPosition   = FormStartPosition.CenterScreen;
        BackColor       = LightBg;
        Font            = new Font("Segoe UI", 9f);
    }

    // ── Header ────────────────────────────────────────────────
    void BuildHeader()
    {
        var header = new Panel { Dock = DockStyle.Top, Height = HeaderH, BackColor = NavyDark };
        header.Controls.Add(new Panel { Dock = DockStyle.Bottom, Height = 3, BackColor = Gold });

        _lblTitle = new Label {
            ForeColor = Color.White,
            Font      = new Font("Segoe UI", 13f, FontStyle.Bold),
            Location  = new Point(Pad, 9),
            AutoSize  = true
        };
        _lblSub = new Label {
            ForeColor = Color.FromArgb(160, 190, 230),
            Font      = new Font("Segoe UI", 9f),
            Location  = new Point(Pad + 2, 40),
            AutoSize  = true
        };

        header.Controls.Add(_lblTitle);
        header.Controls.Add(_lblSub);
        Controls.Add(header);
    }

    // ── Footer ────────────────────────────────────────────────
    void BuildFooter()
    {
        var footer = new Panel { Dock = DockStyle.Bottom, Height = FooterH, BackColor = Color.FromArgb(215, 222, 240) };
        footer.Controls.Add(new Panel { Dock = DockStyle.Top, Height = 1, BackColor = Color.FromArgb(180, 188, 212) });

        // Right-aligned: Cancel | Next | Back (right to left)
        int right = W - 14;
        _btnCancel = Btn(footer, "Cancel", right - 88,           9);
        _btnNext   = Btn(footer, "Next >", right - 88 - 8 - 88,  9);
        _btnBack   = Btn(footer, "< Back", right - 88 - 8 - 88 - 8 - 88, 9);

        _btnNext.BackColor = NavyMid;
        _btnNext.ForeColor = Color.White;
        _btnNext.FlatStyle = FlatStyle.Flat;
        _btnNext.FlatAppearance.BorderColor = NavyDark;

        _btnCancel.Click += (s, e) => OnCancel();
        _btnBack.Click   += (s, e) => ShowPage(_page - 1);
        _btnNext.Click   += (s, e) => OnNext();

        Controls.Add(footer);
    }

    static Button Btn(Panel parent, string text, int x, int y)
    {
        var b = new Button { Text = text, Size = new Size(88, 28), Location = new Point(x, y), FlatStyle = FlatStyle.System };
        parent.Controls.Add(b);
        return b;
    }

    // ── Page 0: Welcome ───────────────────────────────────────
    void BuildPage0()
    {
        _p0 = MakePage();

        Lbl(_p0, "Welcome to BattleshipJava!", Pad, 18, ContentW, 30,
            new Font("Segoe UI", 14f, FontStyle.Bold), NavyDark);
        HSep(_p0, 54);
        Lbl(_p0,
            "This wizard will install BattleshipJava on your computer.\n\n" +
            "Features:\n" +
            "   \u2022  Single-player vs AI  (Easy / Medium / Hard)\n" +
            "   \u2022  Local network multiplayer\n" +
            "   \u2022  Sound effects and music\n\n" +
            "Requirements:\n" +
            "   \u2022  Java 17 or later  (prompted on first launch if missing)\n" +
            "   \u2022  Windows 10 / 11\n\n" +
            "Click  Next >  to continue.",
            Pad, 62, ContentW, 270,
            new Font("Segoe UI", 10f), Color.FromArgb(30, 40, 70));
    }

    // ── Page 1: Options ───────────────────────────────────────
    void BuildPage1()
    {
        _p1 = MakePage();

        Lbl(_p1, "Install Location", Pad, 16, ContentW, 26,
            new Font("Segoe UI", 11f, FontStyle.Bold), NavyDark);
        Lbl(_p1, "Install BattleshipJava to this folder:", Pad, 52, 420, 20,
            new Font("Segoe UI", 9.5f), Color.FromArgb(50, 60, 80));

        _txtDir = new TextBox {
            Location = new Point(Pad, 74),
            Size     = new Size(ContentW - 100, 24),
            Font     = new Font("Segoe UI", 10f),
            Text     = _dstDir
        };
        _p1.Controls.Add(_txtDir);

        var btnBrowse = new Button {
            Text      = "Browse\u2026",
            Location  = new Point(Pad + ContentW - 96, 72),
            Size      = new Size(96, 28),
            FlatStyle = FlatStyle.System
        };
        btnBrowse.Click += (s, e) => {
            using (var dlg = new FolderBrowserDialog()) {
                dlg.Description  = "Choose install folder";
                dlg.SelectedPath = _txtDir.Text;
                if (dlg.ShowDialog() == DialogResult.OK)
                    _txtDir.Text = dlg.SelectedPath;
            }
        };
        _p1.Controls.Add(btnBrowse);

        HSep(_p1, 114);
        Lbl(_p1, "Shortcuts:", Pad, 124, 200, 20,
            new Font("Segoe UI", 9.5f, FontStyle.Bold), NavyDark);

        _chkDesktop   = Chk(_p1, "Create Desktop shortcut", Pad + 8, 148);
        _chkStartMenu = Chk(_p1, "Add to Start Menu",        Pad + 8, 174);

        Lbl(_p1, "Space required: ~30 MB", Pad, 216, 400, 18,
            new Font("Segoe UI", 8.5f), Color.FromArgb(110, 120, 150));
    }

    // ── Page 2: Installing ────────────────────────────────────
    void BuildPage2()
    {
        _p2 = MakePage();

        Lbl(_p2, "Installing\u2026", Pad, 16, ContentW, 26,
            new Font("Segoe UI", 11f, FontStyle.Bold), NavyDark);

        _lblStatus = Lbl(_p2, "Preparing\u2026", Pad, 50, ContentW, 18,
            new Font("Segoe UI", 9f), Color.FromArgb(50, 60, 80));

        _bar = new ProgressBar {
            Location = new Point(Pad, 72),
            Size     = new Size(ContentW, 20),
            Style    = ProgressBarStyle.Continuous
        };
        _p2.Controls.Add(_bar);

        _log = new RichTextBox {
            Location    = new Point(Pad, 100),
            Size        = new Size(ContentW, 220),
            ReadOnly    = true,
            Font        = new Font("Consolas", 8.5f),
            BackColor   = Color.FromArgb(14, 24, 50),
            ForeColor   = Color.FromArgb(120, 200, 120),
            BorderStyle = BorderStyle.None,
            ScrollBars  = RichTextBoxScrollBars.Vertical
        };
        _p2.Controls.Add(_log);
    }

    // ── Page 3: Done ──────────────────────────────────────────
    void BuildPage3()
    {
        _p3 = MakePage();

        Lbl(_p3, "Installation Complete!", Pad, 18, ContentW, 30,
            new Font("Segoe UI", 14f, FontStyle.Bold), Color.FromArgb(20, 120, 40));
        HSep(_p3, 54);
        Lbl(_p3,
            "BattleshipJava has been successfully installed.\n\n" +
            "You can launch the game from:\n" +
            "   \u2022  The Desktop shortcut (if created)\n" +
            "   \u2022  The Start Menu entry (if created)\n" +
            "   \u2022  Battleship.exe in the install folder\n\n" +
            "On first launch the game will verify Java is installed\n" +
            "and will download JavaFX automatically if needed.",
            Pad, 62, ContentW, 230,
            new Font("Segoe UI", 10f), Color.FromArgb(30, 40, 70));

        _chkLaunch = Chk(_p3, "Launch BattleshipJava now", Pad + 8, 278);
    }

    // ── Helpers ───────────────────────────────────────────────
    Panel MakePage()
    {
        var p = new Panel { Bounds = new Rectangle(0, HeaderH, W, ContentH), BackColor = LightBg, Visible = false };
        Controls.Add(p);
        return p;
    }

    static Label Lbl(Panel p, string text, int x, int y, int w, int h, Font font, Color fg)
    {
        var l = new Label { Text = text, Location = new Point(x, y), Size = new Size(w, h), Font = font, ForeColor = fg, AutoSize = false };
        p.Controls.Add(l);
        return l;
    }

    static CheckBox Chk(Panel p, string text, int x, int y)
    {
        var c = new CheckBox {
            Text      = text,
            Location  = new Point(x, y),
            Size      = new Size(500, 22),
            Font      = new Font("Segoe UI", 9.5f),
            Checked   = true,
            ForeColor = Color.FromArgb(30, 40, 70)
        };
        p.Controls.Add(c);
        return c;
    }

    static void HSep(Panel p, int y)
    {
        p.Controls.Add(new Panel { Location = new Point(Pad, y), Size = new Size(W - Pad * 2, 1), BackColor = Color.FromArgb(200, 210, 230) });
    }

    // ── Navigation ────────────────────────────────────────────
    void ShowPage(int idx)
    {
        foreach (var p in _pages) p.Visible = false;
        _page = idx;
        _pages[idx].Visible = true;

        _btnBack.Enabled   = true;
        _btnNext.Enabled   = true;
        _btnCancel.Visible = true;
        _btnNext.Text      = "Next >";

        switch (idx)
        {
            case 0:
                _lblTitle.Text   = "Welcome";
                _lblSub.Text     = "BattleshipJava Installer";
                _btnBack.Enabled = false;
                break;
            case 1:
                _lblTitle.Text = "Install Location";
                _lblSub.Text   = "Choose where to install BattleshipJava";
                _btnNext.Text  = "Install";
                break;
            case 2:
                _lblTitle.Text     = "Installing";
                _lblSub.Text       = "Please wait\u2026";
                _btnBack.Enabled   = false;
                _btnNext.Enabled   = false;
                _btnCancel.Visible = false;
                BeginInstall();
                break;
            case 3:
                _lblTitle.Text     = "Complete";
                _lblSub.Text       = "BattleshipJava is ready to play!";
                _btnBack.Enabled   = false;
                _btnNext.Text      = "Finish";
                _btnCancel.Visible = false;
                break;
        }
    }

    void OnNext()
    {
        if (_page == 1)
        {
            _dstDir = _txtDir.Text.Trim();
            if (string.IsNullOrEmpty(_dstDir)) {
                MessageBox.Show("Please specify an install folder.", "BattleshipJava",
                    MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }
            _createDesktop   = _chkDesktop.Checked;
            _createStartMenu = _chkStartMenu.Checked;
        }
        if (_page == 3) {
            if (_chkLaunch.Checked) LaunchGame();
            Close();
            return;
        }
        ShowPage(_page + 1);
    }

    void OnCancel()
    {
        if (_page == 2) return;
        if (MessageBox.Show("Cancel the installation?", "BattleshipJava",
            MessageBoxButtons.YesNo, MessageBoxIcon.Question) == DialogResult.Yes)
            Close();
    }

    // ── Install worker ────────────────────────────────────────
    void BeginInstall()
    {
        _lastPct = 0;
        _worker  = new BackgroundWorker { WorkerReportsProgress = true };
        _worker.DoWork             += DoInstall;
        _worker.ProgressChanged    += OnProgress;
        _worker.RunWorkerCompleted += OnDone;
        _worker.RunWorkerAsync();
    }

    void Report(BackgroundWorker w, int pct, string msg)
    {
        if (pct >= 0) _lastPct = pct;
        w.ReportProgress(_lastPct, msg);
    }

    void DoInstall(object sender, DoWorkEventArgs e)
    {
        var w = (BackgroundWorker)sender;

        Report(w, 0, "Creating install directory\u2026");
        Directory.CreateDirectory(_dstDir);

        // Only copy the compiled output and the launcher exe — not the stale run.ps1
        string[] syncFolders = { "out", "src" };
        string[] syncFiles   = { "Battleship.exe" };

        int total = 0;
        foreach (var folder in syncFolders) {
            var d = Path.Combine(_srcDir, folder);
            if (Directory.Exists(d))
                total += Directory.GetFiles(d, "*", SearchOption.AllDirectories).Length;
        }
        foreach (var f in syncFiles)
            if (File.Exists(Path.Combine(_srcDir, f))) total++;
        total += 1; // for run.ps1 we write ourselves
        if (total < 1) total = 1;

        int done = 0;

        foreach (var folder in syncFolders) {
            string srcF = Path.Combine(_srcDir, folder);
            string dstF = Path.Combine(_dstDir, folder);
            if (!Directory.Exists(srcF)) continue;

            Report(w, -1, "Copying " + folder + "\\...");

            foreach (var srcFile in Directory.GetFiles(srcF, "*", SearchOption.AllDirectories)) {
                string rel     = srcFile.Substring(srcF.Length).TrimStart('\\', '/');
                string dstFile = Path.Combine(dstF, rel);
                Directory.CreateDirectory(Path.GetDirectoryName(dstFile));
                File.Copy(srcFile, dstFile, true);
                done++;
                Report(w, done * 80 / total, null);
            }
        }

        foreach (var fileName in syncFiles) {
            string srcFile = Path.Combine(_srcDir, fileName);
            if (!File.Exists(srcFile)) continue;
            Report(w, -1, "Copying " + fileName + "\u2026");
            File.Copy(srcFile, Path.Combine(_dstDir, fileName), true);
            done++;
            Report(w, done * 80 / total, null);
        }

        // Write a clean launcher run.ps1 that works from any install location
        Report(w, 82, "Writing launcher\u2026");
        File.WriteAllText(Path.Combine(_dstDir, "run.ps1"), LauncherScript());
        done++;
        Report(w, done * 80 / total, null);

        // Determine launch target
        string exePath = Path.Combine(_dstDir, "Battleship.exe");
        if (!File.Exists(exePath)) {
            Report(w, -1, "Battleship.exe not found \u2014 writing Battleship.bat fallback.");
            exePath = Path.Combine(_dstDir, "Battleship.bat");
            File.WriteAllText(exePath,
                "@echo off\r\n" +
                "powershell -ExecutionPolicy Bypass -NoProfile -WindowStyle Hidden " +
                "-File \"%~dp0run.ps1\"\r\n");
        }

        Report(w, 90, "Creating shortcuts\u2026");

        if (_createDesktop) {
            CreateShortcut(
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory), "BattleshipJava.lnk"),
                exePath, _dstDir);
            Report(w, -1, "  Desktop shortcut created.");
        }
        if (_createStartMenu) {
            string programs = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.StartMenu), "Programs");
            Directory.CreateDirectory(programs);
            CreateShortcut(Path.Combine(programs, "BattleshipJava.lnk"), exePath, _dstDir);
            Report(w, -1, "  Start Menu entry created.");
        }

        Report(w, 100, "Done!");
    }

    // ── Launcher script written to every install ───────────────
    static string LauncherScript()
    {
        return
            "# BattleshipJava launcher — auto-generated by Setup\r\n" +
            "$ErrorActionPreference = 'Stop'\r\n" +
            "$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path\r\n" +
            "$outDir    = Join-Path $scriptDir 'out'\r\n" +
            "\r\n" +
            "function Find-JavaFX {\r\n" +
            "    function Has-Dlls($p) {\r\n" +
            "        if (Get-ChildItem $p -Filter 'prism_d3d.dll' -EA SilentlyContinue | Select-Object -First 1) { return $true }\r\n" +
            "        $bin = Join-Path (Split-Path $p -Parent) 'bin'\r\n" +
            "        if (Get-ChildItem $bin -Filter 'prism_d3d.dll' -EA SilentlyContinue | Select-Object -First 1) { return $true }\r\n" +
            "        return $false\r\n" +
            "    }\r\n" +
            "    function Valid-SDK($p) { (Test-Path (Join-Path $p 'javafx.controls.jar')) -and (Has-Dlls $p) }\r\n" +
            "    foreach ($p in @(\"$env:LOCALAPPDATA\\javafx\\lib\", \"$env:ProgramFiles\\javafx\\lib\")) {\r\n" +
            "        if (Valid-SDK $p) { return $p }\r\n" +
            "    }\r\n" +
            "    foreach ($root in @(\"$env:LOCALAPPDATA\\javafx\", \"$env:USERPROFILE\\Downloads\")) {\r\n" +
            "        $hit = Get-ChildItem $root -Recurse -Filter 'javafx.controls.jar' -EA SilentlyContinue |\r\n" +
            "               Where-Object { Valid-SDK $_.DirectoryName } | Select-Object -First 1\r\n" +
            "        if ($hit) { return $hit.DirectoryName }\r\n" +
            "    }\r\n" +
            "    return $null\r\n" +
            "}\r\n" +
            "\r\n" +
            "function Show-Msg($msg, $title) {\r\n" +
            "    Add-Type -AssemblyName System.Windows.Forms\r\n" +
            "    [System.Windows.Forms.MessageBox]::Show($msg, $title, 0, 48) | Out-Null\r\n" +
            "}\r\n" +
            "\r\n" +
            "# Java check\r\n" +
            "$javaOk = $false\r\n" +
            "try {\r\n" +
            "    $psi = New-Object System.Diagnostics.ProcessStartInfo\r\n" +
            "    $psi.FileName = 'java'; $psi.Arguments = '-version'\r\n" +
            "    $psi.UseShellExecute = $false\r\n" +
            "    $psi.RedirectStandardError = $true; $psi.RedirectStandardOutput = $true\r\n" +
            "    $psi.CreateNoWindow = $true\r\n" +
            "    $p = [System.Diagnostics.Process]::Start($psi)\r\n" +
            "    $out = $p.StandardError.ReadToEnd() + $p.StandardOutput.ReadToEnd()\r\n" +
            "    $p.WaitForExit()\r\n" +
            "    if ($out -match '\"(\\d+)') { $javaOk = ([int]$Matches[1] -ge 17) }\r\n" +
            "} catch { $javaOk = $false }\r\n" +
            "if (-not $javaOk) {\r\n" +
            "    Show-Msg \"Java 17 or later is required.`nGet it free at https://adoptium.net\" 'BattleshipJava'\r\n" +
            "    exit 1\r\n" +
            "}\r\n" +
            "\r\n" +
            "# JavaFX check / auto-download\r\n" +
            "$jfx = Find-JavaFX\r\n" +
            "if (-not $jfx) {\r\n" +
            "    Add-Type -AssemblyName System.Windows.Forms\r\n" +
            "    $r = [System.Windows.Forms.MessageBox]::Show(\r\n" +
            "        \"JavaFX SDK was not found.`nDownload it now? (~70 MB)\",\r\n" +
            "        'BattleshipJava', 4, 32)\r\n" +
            "    if ($r -ne 6) { exit 1 }\r\n" +
            "    $dlDir = Join-Path $env:LOCALAPPDATA 'javafx'\r\n" +
            "    $zip   = Join-Path $dlDir 'openjfx.zip'\r\n" +
            "    New-Item -ItemType Directory -Force -Path $dlDir | Out-Null\r\n" +
            "    Invoke-WebRequest 'https://download2.gluonhq.com/openjfx/21.0.3/openjfx-21.0.3_windows-x64_bin-sdk.zip' " +
                        "-OutFile $zip -UseBasicParsing\r\n" +
            "    Expand-Archive $zip (Join-Path $dlDir 'sdk') -Force\r\n" +
            "    Remove-Item $zip -EA SilentlyContinue\r\n" +
            "    $jfx = Find-JavaFX\r\n" +
            "    if (-not $jfx) { Show-Msg 'JavaFX installation failed.' 'BattleshipJava'; exit 1 }\r\n" +
            "}\r\n" +
            "\r\n" +
            "# Launch\r\n" +
            "Start-Process 'java' -ArgumentList @(\r\n" +
            "    '--enable-native-access=javafx.graphics,javafx.media',\r\n" +
            "    '--module-path', \"`\"$jfx`\"\",\r\n" +
            "    '--add-modules', 'javafx.controls,javafx.media',\r\n" +
            "    '-cp', \"`\"$outDir`\"\",\r\n" +
            "    'Main'\r\n" +
            ")\r\n";
    }

    static void CreateShortcut(string lnkPath, string targetPath, string workDir)
    {
        string tmp = Path.Combine(Path.GetTempPath(),
            "bs_setup_" + Guid.NewGuid().ToString("N") + ".ps1");
        File.WriteAllText(tmp,
            "$ws = New-Object -ComObject WScript.Shell\n" +
            "$s  = $ws.CreateShortcut('" + lnkPath.Replace("'", "''")    + "')\n" +
            "$s.TargetPath       = '" + targetPath.Replace("'", "''") + "'\n" +
            "$s.WorkingDirectory = '" + workDir.Replace("'", "''")    + "'\n" +
            "$s.Description     = 'BattleshipJava'\n" +
            "$s.Save()\n");
        try {
            var p = Process.Start(new ProcessStartInfo("powershell.exe",
                "-NoProfile -ExecutionPolicy Bypass -File \"" + tmp + "\"") {
                CreateNoWindow  = true,
                UseShellExecute = false
            });
            if (p != null) p.WaitForExit(15000);
        } finally {
            try { File.Delete(tmp); } catch { }
        }
    }

    void OnProgress(object sender, ProgressChangedEventArgs e)
    {
        if (e.ProgressPercentage >= 0 && e.ProgressPercentage <= 100)
            _bar.Value = e.ProgressPercentage;

        string msg = e.UserState as string;
        if (!string.IsNullOrEmpty(msg)) {
            _lblStatus.Text = msg;
            _log.AppendText(msg + "\n");
            _log.ScrollToCaret();
        }
    }

    void OnDone(object sender, RunWorkerCompletedEventArgs e)
    {
        if (e.Error != null) {
            _lblTitle.Text  = "Installation Failed";
            _lblSub.Text    = "An error occurred.";
            _lblStatus.Text = "Error: " + e.Error.Message;
            _log.SelectionColor = Color.FromArgb(230, 80, 80);
            _log.AppendText("\n[ERROR] " + e.Error.Message + "\n");
            _btnCancel.Text    = "Close";
            _btnCancel.Visible = true;
            return;
        }
        ShowPage(3);
    }

    void LaunchGame()
    {
        string exe = Path.Combine(_dstDir, "Battleship.exe");
        if (!File.Exists(exe)) exe = Path.Combine(_dstDir, "Battleship.bat");
        if (File.Exists(exe))
            try { Process.Start(new ProcessStartInfo(exe) { UseShellExecute = true }); }
            catch { }
    }
}
