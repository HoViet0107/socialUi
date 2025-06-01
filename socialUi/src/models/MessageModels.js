export class MessageModels {
    constructor(data = {}) {
        this.content = data.content || '';
        this.status = data.status || 'SENDING';
        this.mediaType = data.mediaType || 'TEXT';
        this.replyToMessageId = data.replyToMessageId || null;
        this.file = data.file || null;
    }    toFormData() {
        const formData = new FormData();
        
        // Thêm các trường bắt buộc
        formData.append('content', this.content || '');
        formData.append('status', this.status || 'SENDING');
        formData.append('mediaType', this.mediaType || 'TEXT');
        
        // Chỉ thêm các trường không bắt buộc khi có giá trị
        if (this.replyToMessageId) {
            formData.append('replyToMessageId', String(this.replyToMessageId));
        }
        
        // Chỉ thêm file khi có file và mediaType không phải TEXT
        if (this.file && this.mediaType !== 'TEXT') {
            formData.append('file', this.file);
        } else {
            // Nếu không có file, không gửi field file
            // formData.append('file', null); // Không gửi file null
        }
        
        return formData;
    }
}