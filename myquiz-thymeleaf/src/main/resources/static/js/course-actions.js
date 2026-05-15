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

