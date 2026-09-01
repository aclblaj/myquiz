function toggleRecomputeConfirm(courseId) {
    var confirmBox = document.getElementById('recompute-confirm-' + courseId);
    if (!confirmBox) {
        return;
    }
    confirmBox.style.display = confirmBox.style.display === 'none' ? 'block' : 'none';
}

function hideRecomputeConfirm(courseId) {
    var confirmBox = document.getElementById('recompute-confirm-' + courseId);
    if (confirmBox) {
        confirmBox.style.display = 'none';
    }
}

function submitRecompute(courseId) {
    var form = document.getElementById('recompute-form-' + courseId);
    if (form) {
        form.submit();
    }
}

function toggleDeleteDuplicatesConfirm(courseId) {
    var confirmBox = document.getElementById('delete-duplicates-confirm-' + courseId);
    if (!confirmBox) {
        return;
    }
    confirmBox.style.display = confirmBox.style.display === 'none' ? 'block' : 'none';
}

function hideDeleteDuplicatesConfirm(courseId) {
    var confirmBox = document.getElementById('delete-duplicates-confirm-' + courseId);
    if (confirmBox) {
        confirmBox.style.display = 'none';
    }
}

function submitDeleteDuplicates(courseId) {
    var form = document.getElementById('delete-duplicates-form-' + courseId);
    if (form) {
        // Disable the button to prevent multiple submissions
        var button = event.target;
        if (button) {
            button.disabled = true;
        }
        form.submit();
    }
}

