class Metronome {
    constructor() {
        this.go = false;
        this.tempoBpm = 60;
        this.soundNum = 1;
        this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
    }

    setTempo(bpm) {
        this.tempoBpm = bpm;
    }

    setSound(number) {
        this.soundNum = number;
    }

    loadMetronomeSounds(filenames) {
        const urls = filenames.map(name => 'assets/audio/' + name);
        this.soundFiles = new SoundFiles(this.audioContext, urls);
        this.soundFiles.load();
    }

    toggle() {
        const metronome = $('#accurateMetronome');
        if (this.go) {
            metronome.removeClass('active');
            this.go = false;
        } else {
            metronome.addClass('active');
            this.go = true;
            this.playMetronome();
        }
    }

    playMetronome() {
        const metronome = this;
        let nextStart = this.audioContext.currentTime;

        function schedule() {
            if (!metronome.go) return;

            const bufIndex = metronome.soundNum - 1;
            if (bufIndex < metronome.soundFiles.buffers.length) {
                const bps = metronome.tempoBpm === 0 ? 1 : metronome.tempoBpm / 60.0;
                nextStart += 1 / bps;
                const source = metronome.audioContext.createBufferSource();
                source.buffer = metronome.soundFiles.buffers[bufIndex];
                source.connect(metronome.audioContext.destination);
                source.onended = schedule;
                source.start(nextStart);
            } else alert('Sound files are not yet loaded')
        }

        schedule();
    }
}

class SoundFiles {
    constructor(context, urlList) {
        this.context = context;
        this.urlList = urlList;
        this.buffers = [];
    }

    loadBuffer(url, index) {
        const self = this;
        const xhr = new XMLHttpRequest();
        xhr.open("GET", url, true);
        xhr.responseType = "arraybuffer";
        xhr.onload = () => self.context.decodeAudioData(xhr.response,
            (buffer) => self.buffers[index] = buffer,
            (error) => console.error('decodeAudioData error', error));
        xhr.send();
    };

    load() {
        this.urlList.forEach((name, index) => this.loadBuffer(name, index));
    }
}
